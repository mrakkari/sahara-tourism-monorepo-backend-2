package com.camping.duneinsolite.service.impl;

import com.camping.duneinsolite.config.RabbitMQConfig;
import com.camping.duneinsolite.dto.message.NotificationMessage;
import com.camping.duneinsolite.dto.request.ReservationRequest;
import com.camping.duneinsolite.dto.request.ReservationUpdateRequest;
import com.camping.duneinsolite.dto.request.TourTypeSelectionRequest;
import com.camping.duneinsolite.dto.response.ReservationResponse;
import com.camping.duneinsolite.exception.ReservationStatusException;
import com.camping.duneinsolite.mapper.ReservationMapper;
import com.camping.duneinsolite.model.*;
import com.camping.duneinsolite.model.enums.NotificationType;
import com.camping.duneinsolite.model.enums.ReservationStatus;
import com.camping.duneinsolite.model.enums.ReservationType;
import com.camping.duneinsolite.model.enums.UserRole;
import com.camping.duneinsolite.repository.*;
import com.camping.duneinsolite.service.NotificationPublisher;
import com.camping.duneinsolite.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ReservationServiceImpl implements ReservationService {

    private final ReservationRepository      reservationRepository;
    private final UserRepository             userRepository;
    private final TourTypeRepository         tourTypeRepository;
    private final ExtraRepository            extraRepository;
    private final TourRepository             tourRepository;
    private final ReservationMapper          reservationMapper;
    private final NotificationPublisher      notificationPublisher;

    // ─────────────────────────────────────────────────────────────
    // CREATE
    // ─────────────────────────────────────────────────────────────
    @Override
    public ReservationResponse createReservation(ReservationRequest request) {

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found: " + request.getUserId()));

        boolean isPartner = user.getRole() == UserRole.PARTENAIRE;

        int globalAdults   = request.getNumberOfAdults()   != null ? request.getNumberOfAdults()   : 0;
        int globalChildren = request.getNumberOfChildren() != null ? request.getNumberOfChildren() : 0;

        ReservationType type = request.getReservationType();

        // ── Type-level validation ─────────────────────────────────
        switch (type) {
            case HEBERGEMENT -> {
                if (request.getCheckInDate() == null || request.getCheckOutDate() == null) {
                    throw new RuntimeException("Check-in and check-out dates are required for HEBERGEMENT reservations");
                }
                if (request.getTourTypes() == null || request.getTourTypes().isEmpty()) {
                    throw new RuntimeException("At least one tour type is required for HEBERGEMENT reservations");
                }
            }
            case TOURS -> {
                if (request.getTours() == null || request.getTours().isEmpty()) {
                    throw new RuntimeException("A tour selection is required for TOURS reservations");
                }
                if (request.getTours().size() > 1) {
                    throw new RuntimeException("Only one tour can be selected per reservation");
                }
                if (request.getServiceDate() == null) {  // ← serviceDate = departure date for TOURS
                    throw new RuntimeException("Departure date (serviceDate) is required for TOURS reservations");
                }
            }
            case EXTRAS -> {
                if (request.getExtras() == null || request.getExtras().isEmpty()) {
                    throw new RuntimeException("At least one extra is required for EXTRAS reservations");
                }
                if (request.getServiceDate() == null) {
                    throw new RuntimeException("Service date is required for EXTRAS reservations");
                }
            }
        }

        // ── Build base reservation ────────────────────────────────
        Reservation reservation = Reservation.builder()
                .user(user)
                .source(request.getSource())
                .reservationType(type)
                .checkInDate(request.getCheckInDate())
                .checkOutDate(request.getCheckOutDate())
                .serviceDate(request.getServiceDate())
                .groupName(request.getGroupName())
                .groupLeaderName(request.getGroupLeaderName())
                .demandeSpecial(request.getDemandeSpecial())
                .numberOfAdults(globalAdults)
                .numberOfChildren(globalChildren)
                .currency(request.getCurrency() != null ? request.getCurrency() : "USD")
                .promoCode(request.getPromoCode())
                .status(ReservationStatus.PENDING)
                .build();

        // ── HEBERGEMENT — snapshot TourTypes ──────────────────────
        if (type == ReservationType.HEBERGEMENT) {
            long nights = ChronoUnit.DAYS.between(request.getCheckInDate(), request.getCheckOutDate());
            if (nights <= 0) nights = 1;

            boolean singleTourType = request.getTourTypes().size() == 1;

            // Validate per-tourtype people counts when multiple selected
            if (!singleTourType) {
                for (TourTypeSelectionRequest selection : request.getTourTypes()) {
                    int selAdults   = selection.getNumberOfAdults()   != null ? selection.getNumberOfAdults()   : 0;
                    int selChildren = selection.getNumberOfChildren() != null ? selection.getNumberOfChildren() : 0;

                    if (selAdults > globalAdults) {
                        throw new RuntimeException(
                                "Tour type adults (" + selAdults + ") cannot exceed group adults (" + globalAdults + ")"
                        );
                    }
                    if (selChildren > globalChildren) {
                        throw new RuntimeException(
                                "Tour type children (" + selChildren + ") cannot exceed group children (" + globalChildren + ")"
                        );
                    }
                }

                int totalSelAdults = request.getTourTypes().stream()
                        .mapToInt(t -> t.getNumberOfAdults() != null ? t.getNumberOfAdults() : 0).sum();
                int totalSelChildren = request.getTourTypes().stream()
                        .mapToInt(t -> t.getNumberOfChildren() != null ? t.getNumberOfChildren() : 0).sum();

                if (totalSelAdults < globalAdults) {
                    throw new RuntimeException(
                            "Total adults across all tour types (" + totalSelAdults + ") " +
                                    "cannot be less than group adults (" + globalAdults + "). " +
                                    "Every person must be assigned to at least one tour type."
                    );
                }
                if (totalSelChildren < globalChildren) {
                    throw new RuntimeException(
                            "Total children across all tour types (" + totalSelChildren + ") " +
                                    "cannot be less than group children (" + globalChildren + "). " +
                                    "Every child must be assigned to at least one tour type."
                    );
                }
            }

            for (TourTypeSelectionRequest selection : request.getTourTypes()) {
                TourType tourType = tourTypeRepository.findById(selection.getTourTypeId())
                        .orElseThrow(() -> new RuntimeException("TourType not found: " + selection.getTourTypeId()));

                int adults   = singleTourType ? globalAdults   : (selection.getNumberOfAdults()   != null ? selection.getNumberOfAdults()   : 0);
                int children = singleTourType ? globalChildren : (selection.getNumberOfChildren() != null ? selection.getNumberOfChildren() : 0);

                double adultPrice = isPartner ? tourType.getPartnerAdultPrice() : tourType.getPassengerAdultPrice();
                double childPrice = isPartner ? tourType.getPartnerChildPrice() : tourType.getPassengerChildPrice();

                ReservationTourType snapshot = ReservationTourType.builder()
                        .name(tourType.getName())
                        .description(tourType.getDescription())
                        .duration(tourType.getDuration())
                        .adultPrice(adultPrice)
                        .childPrice(childPrice)
                        .numberOfAdults(adults)
                        .numberOfChildren(children)
                        .numberOfNights((int) nights)
                        .build();

                reservation.addTourType(snapshot);
            }

            reservation.setTotalAmount(reservation.calculateTotalTourTypesAmount());
        }

        // ── TOURS — snapshot Tour ─────────────────────────────────
        if (type == ReservationType.TOURS) {
            var selection = request.getTours().get(0);

            Tour tour = tourRepository.findById(selection.getTourId())
                    .orElseThrow(() -> new RuntimeException("Tour not found: " + selection.getTourId()));

            double adultPrice = isPartner ? tour.getPartnerAdultPrice() : tour.getPassengerAdultPrice();
            double childPrice = isPartner ? tour.getPartnerChildPrice() : tour.getPassengerChildPrice();
            double totalPrice = (globalAdults * adultPrice) + (globalChildren * childPrice);

            ReservationTour reservationTour = ReservationTour.builder()
                    .name(tour.getName())
                    .description(tour.getDescription())
                    .duration(tour.getDuration())
                    .adultPrice(adultPrice)
                    .childPrice(childPrice)
                    .numberOfAdults(globalAdults)
                    .numberOfChildren(globalChildren)
                    .departureDate(request.getServiceDate())  // ← serviceDate used as departureDate
                    .totalPrice(totalPrice)
                    .build();

            reservation.addTour(reservationTour);
            reservation.setTotalAmount(reservation.calculateTotalToursAmount());
        }

        // ── EXTRAS only — totalAmount is null ─────────────────────
        if (type == ReservationType.EXTRAS) {
            reservation.setTotalAmount(null);
        }

        // ── Participants (all types) ───────────────────────────────
        if (request.getParticipants() != null) {
            request.getParticipants().forEach(p -> {
                Participant participant = Participant.builder()
                        .fullName(p.getFullName())
                        .age(p.getAge())
                        .isAdult(p.getIsAdult())
                        .build();
                reservation.addParticipant(participant);
            });
        }

        // ── Extras (all types) ────────────────────────────────────
        if (request.getExtras() != null) {
            request.getExtras().forEach(e -> {
                Extra catalog = extraRepository.findById(e.getExtraId())
                        .orElseThrow(() -> new RuntimeException("Extra not found: " + e.getExtraId()));

                ReservationExtra extra = ReservationExtra.builder()
                        .name(catalog.getName())
                        .description(catalog.getDescription())
                        .duration(catalog.getDuration())
                        .quantity(e.getQuantity())
                        .unitPrice(catalog.getUnitPrice())
                        .totalPrice(catalog.getUnitPrice() * e.getQuantity())
                        .isActive(true)
                        .build();
                reservation.addExtra(extra);
            });
        }

        reservation.setTotalExtrasAmount(reservation.calculateTotalExtrasAmount());

        // ── Save and notify ───────────────────────────────────────
        Reservation savedReservation = reservationRepository.save(reservation);

        notificationPublisher.publish(
                RabbitMQConfig.RESERVATION_CREATED,
                NotificationMessage.builder()
                        .targetRoles(List.of(UserRole.ADMIN))
                        .type(NotificationType.RESERVATION_CREATED)
                        .reservationId(savedReservation.getReservationId())
                        .title("Nouvelle réservation")
                        .message("Le groupe \"" + savedReservation.getGroupName()
                                + "\" a soumis une demande de réservation.")
                        .build()
        );

        return reservationMapper.toResponse(savedReservation);
    }

    // ─────────────────────────────────────────────────────────────
    // ALL OTHER METHODS — UNTOUCHED FROM ORIGINAL
    // ─────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public ReservationResponse getReservationById(UUID reservationId) {
        return reservationMapper.toResponse(findById(reservationId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReservationResponse> getAllReservations() {
        return reservationRepository.findAll().stream()
                .map(reservationMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReservationResponse> getReservationsByUser(UUID userId) {
        return reservationRepository.findByUserUserId(userId).stream()
                .map(reservationMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReservationResponse> getReservationsByStatus(ReservationStatus status) {
        return reservationRepository.findByStatus(status).stream()
                .map(reservationMapper::toResponse).toList();
    }

    @Override
    public ReservationResponse updateReservationStatus(UUID reservationId, ReservationStatus status, String rejectionReason) {
        Reservation reservation = findById(reservationId);

        ReservationStatus current = reservation.getStatus();

        if (current == ReservationStatus.COMPLETED) {
            throw new ReservationStatusException("This reservation is already completed and cannot be modified.");
        }
        if (current == ReservationStatus.CANCELLED) {
            throw new ReservationStatusException("This reservation has already been cancelled and cannot be modified.");
        }
// ✅ REPLACE WITH THIS
        if (current == ReservationStatus.CHECKED_IN && status != ReservationStatus.COMPLETED) {
            throw new ReservationStatusException("A checked-in reservation can only be marked as completed.");
        }
        if (current == ReservationStatus.REJECTED) {
            throw new ReservationStatusException("This reservation has been rejected and cannot be modified.");
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdminOrCamping = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_CAMPING"));

        if (!isAdminOrCamping) {
            if (status != ReservationStatus.CANCELLED) {
                throw new AccessDeniedException("You are not authorized to set this status. Only cancellation is allowed.");
            }

            LocalDateTime cancellationDeadline = reservation.getCheckInDate()
                    .atStartOfDay()
                    .minusHours(48);

            if (LocalDateTime.now().isAfter(cancellationDeadline)) {
                throw new ReservationStatusException(
                        "Cancellation is no longer possible. Reservations must be cancelled " +
                                "at least 48 hours before the check-in date (" + reservation.getCheckInDate() + ")."
                );
            }
        }

        reservation.setStatus(status);
        if (status == ReservationStatus.REJECTED) {
            reservation.setRejectionReason(rejectionReason);
        }

        Reservation savedReservation = reservationRepository.save(reservation);

        if (status == ReservationStatus.CONFIRMED) {
            notificationPublisher.publish(
                    RabbitMQConfig.RESERVATION_CONFIRMED,
                    NotificationMessage.builder()
                            .targetUserId(savedReservation.getUser().getUserId())
                            .type(NotificationType.RESERVATION_CONFIRMED)
                            .reservationId(savedReservation.getReservationId())
                            .title("Réservation confirmée")
                            .message("Votre réservation pour le groupe \""
                                    + savedReservation.getGroupName() + "\" a été confirmée.")
                            .build()
            );
            notificationPublisher.publish(
                    RabbitMQConfig.RESERVATION_CONFIRMED,
                    NotificationMessage.builder()
                            .targetRoles(List.of(UserRole.CAMPING))
                            .type(NotificationType.RESERVATION_CONFIRMED)
                            .reservationId(savedReservation.getReservationId())
                            .title("Nouvelle réservation confirmée")
                            .message("Le groupe \"" + savedReservation.getGroupName()
                                    + "\" arrive le " + savedReservation.getCheckInDate())
                            .build()
            );
        }

        if (status == ReservationStatus.REJECTED) {
            notificationPublisher.publish(
                    RabbitMQConfig.RESERVATION_REJECTED,
                    NotificationMessage.builder()
                            .targetUserId(savedReservation.getUser().getUserId())
                            .type(NotificationType.RESERVATION_REJECTED)
                            .title("Réservation rejetée")
                            .reservationId(savedReservation.getReservationId())
                            .message("Votre réservation pour le groupe \""
                                    + savedReservation.getGroupName() + "\" a été rejetée."
                                    + (savedReservation.getRejectionReason() != null
                                    ? " Raison: " + savedReservation.getRejectionReason() : ""))
                            .build()
            );
        }

        return reservationMapper.toResponse(savedReservation);
    }

    @Override
    public ReservationResponse updateReservation(UUID reservationId, ReservationUpdateRequest request) {
        Reservation reservation = findById(reservationId);

        Jwt jwt = (Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String email = jwt.getClaim("email");

        User authenticatedUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));

        if (!reservation.getUser().getUserId().equals(authenticatedUser.getUserId())) {
            throw new AccessDeniedException("You can only edit your own reservations.");
        }

        if (reservation.getStatus() == ReservationStatus.CHECKED_IN  ||
                reservation.getStatus() == ReservationStatus.COMPLETED   ||
                reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new IllegalStateException("Cannot edit a reservation with status: " + reservation.getStatus());
        }

        if (request.getCheckInDate()     != null) reservation.setCheckInDate(request.getCheckInDate());
        if (request.getCheckOutDate()    != null) reservation.setCheckOutDate(request.getCheckOutDate());
        if (request.getGroupName()       != null) reservation.setGroupName(request.getGroupName());
        if (request.getGroupLeaderName() != null) reservation.setGroupLeaderName(request.getGroupLeaderName());
        if (request.getDemandeSpecial()  != null) reservation.setDemandeSpecial(request.getDemandeSpecial());
        if (request.getPromoCode()       != null) reservation.setPromoCode(request.getPromoCode());
        if (request.getCurrency()        != null) reservation.setCurrency(request.getCurrency());
        if (request.getNumberOfAdults()   != null) reservation.setNumberOfAdults(request.getNumberOfAdults());
        if (request.getNumberOfChildren() != null) reservation.setNumberOfChildren(request.getNumberOfChildren());

        if (reservation.getStatus() == ReservationStatus.CONFIRMED ||
                reservation.getStatus() == ReservationStatus.REJECTED) {
            reservation.setStatus(ReservationStatus.PENDING);
            reservation.setRejectionReason(null);
        }

        long nights = ChronoUnit.DAYS.between(reservation.getCheckInDate(), reservation.getCheckOutDate());
        if (nights <= 0) nights = 1;

        boolean isPartner = authenticatedUser.getRole() == UserRole.PARTENAIRE;

        if (request.getTourTypes() != null && !request.getTourTypes().isEmpty()) {
            int globalAdults   = reservation.getNumberOfAdults();
            int globalChildren = reservation.getNumberOfChildren();
            boolean singleTourType = request.getTourTypes().size() == 1;

            if (!singleTourType) {
                for (TourTypeSelectionRequest selection : request.getTourTypes()) {
                    int selAdults   = selection.getNumberOfAdults()   != null ? selection.getNumberOfAdults()   : 0;
                    int selChildren = selection.getNumberOfChildren() != null ? selection.getNumberOfChildren() : 0;

                    if (selAdults > globalAdults) throw new RuntimeException(
                            "Tour type adults (" + selAdults + ") cannot exceed group adults (" + globalAdults + ")"
                    );
                    if (selChildren > globalChildren) throw new RuntimeException(
                            "Tour type children (" + selChildren + ") cannot exceed group children (" + globalChildren + ")"
                    );
                }

                int totalSelAdults = request.getTourTypes().stream()
                        .mapToInt(t -> t.getNumberOfAdults() != null ? t.getNumberOfAdults() : 0).sum();
                int totalSelChildren = request.getTourTypes().stream()
                        .mapToInt(t -> t.getNumberOfChildren() != null ? t.getNumberOfChildren() : 0).sum();

                if (totalSelAdults < globalAdults) throw new RuntimeException(
                        "Total adults across all tour types (" + totalSelAdults + ") cannot be less than group adults (" + globalAdults + ")."
                );
                if (totalSelChildren < globalChildren) throw new RuntimeException(
                        "Total children across all tour types (" + totalSelChildren + ") cannot be less than group children (" + globalChildren + ")."
                );
            }

            reservation.getTourTypes().clear();

            for (TourTypeSelectionRequest selection : request.getTourTypes()) {
                TourType tourType = tourTypeRepository.findById(selection.getTourTypeId())
                        .orElseThrow(() -> new RuntimeException("TourType not found: " + selection.getTourTypeId()));

                int adults   = singleTourType ? globalAdults   : (selection.getNumberOfAdults()   != null ? selection.getNumberOfAdults()   : 0);
                int children = singleTourType ? globalChildren : (selection.getNumberOfChildren() != null ? selection.getNumberOfChildren() : 0);

                double adultPrice = isPartner ? tourType.getPartnerAdultPrice() : tourType.getPassengerAdultPrice();
                double childPrice = isPartner ? tourType.getPartnerChildPrice() : tourType.getPassengerChildPrice();

                ReservationTourType snapshot = ReservationTourType.builder()
                        .name(tourType.getName())
                        .description(tourType.getDescription())
                        .duration(tourType.getDuration())
                        .adultPrice(adultPrice)
                        .childPrice(childPrice)
                        .numberOfAdults(adults)
                        .numberOfChildren(children)
                        .numberOfNights((int) nights)
                        .build();

                reservation.addTourType(snapshot);
            }
            reservation.setTotalAmount(reservation.calculateTotalTourTypesAmount());
        }

        if (request.getParticipants() != null) {
            reservation.getParticipants().clear();
            request.getParticipants().forEach(p -> {
                Participant participant = Participant.builder()
                        .fullName(p.getFullName())
                        .age(p.getAge())
                        .isAdult(p.getIsAdult())
                        .build();
                reservation.addParticipant(participant);
            });
        }

        if (request.getExtras() != null) {
            reservation.getExtras().clear();
            request.getExtras().forEach(e -> {
                Extra catalog = extraRepository.findById(e.getExtraId())
                        .orElseThrow(() -> new RuntimeException("Extra not found: " + e.getExtraId()));

                ReservationExtra extra = ReservationExtra.builder()
                        .name(catalog.getName())
                        .description(catalog.getDescription())
                        .duration(catalog.getDuration())
                        .quantity(e.getQuantity())
                        .unitPrice(catalog.getUnitPrice())
                        .totalPrice(catalog.getUnitPrice() * e.getQuantity())
                        .isActive(true)
                        .build();
                reservation.addExtra(extra);
            });
            reservation.setTotalExtrasAmount(reservation.calculateTotalExtrasAmount());
        }

        Reservation savedReservation = reservationRepository.save(reservation);

        notificationPublisher.publish(
                RabbitMQConfig.RESERVATION_UPDATED,
                NotificationMessage.builder()
                        .targetRoles(List.of(UserRole.ADMIN))
                        .type(NotificationType.RESERVATION_UPDATED)
                        .reservationId(savedReservation.getReservationId())
                        .title("Réservation modifiée")
                        .message("Le groupe \"" + savedReservation.getGroupName()
                                + "\" a modifié sa réservation. En attente de reconfirmation.")
                        .build()
        );

        return reservationMapper.toResponse(savedReservation);
    }

    @Override
    public void deleteReservation(UUID reservationId) {
        reservationRepository.delete(findById(reservationId));
    }

    @Override
    public List<ReservationResponse> getMyReservations(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        return reservationRepository.findByUserOrderByCreatedAtDesc(user)
                .stream()
                .map(reservationMapper::toResponse)
                .collect(Collectors.toList());
    }

    private Reservation findById(UUID reservationId) {
        return reservationRepository.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("Reservation not found: " + reservationId));
    }
}