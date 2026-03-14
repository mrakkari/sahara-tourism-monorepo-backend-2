package com.camping.duneinsolite.service.impl;

import com.camping.duneinsolite.dto.request.ReservationRequest;
import com.camping.duneinsolite.dto.request.ReservationUpdateRequest;
import com.camping.duneinsolite.dto.request.TourTypeSelectionRequest;
import com.camping.duneinsolite.dto.response.ReservationResponse;
import com.camping.duneinsolite.exception.ReservationStatusException;
import com.camping.duneinsolite.mapper.ReservationMapper;
import com.camping.duneinsolite.model.*;
import com.camping.duneinsolite.model.enums.ReservationStatus;
import com.camping.duneinsolite.model.enums.UserRole;
import com.camping.duneinsolite.repository.*;
import com.camping.duneinsolite.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import java.time.temporal.ChronoUnit;
import org.springframework.security.oauth2.jwt.Jwt;
@Service
@RequiredArgsConstructor
@Transactional
public class ReservationServiceImpl implements ReservationService {

    private final ReservationRepository reservationRepository;
    private final UserRepository userRepository;
    private final TourTypeRepository tourTypeRepository;
    private final ExtraRepository extraRepository;
    private final ReservationMapper reservationMapper;

    @Override
    public ReservationResponse createReservation(ReservationRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found: " + request.getUserId()));

        if (request.getTourTypes() == null || request.getTourTypes().isEmpty()) {
            throw new RuntimeException("At least one tour type is required");
        }

        boolean isPartner = user.getRole() == UserRole.PARTENAIRE;

        // Global group size — the real number of people in this group
        int globalAdults   = request.getNumberOfAdults()   != null ? request.getNumberOfAdults()   : 0;
        int globalChildren = request.getNumberOfChildren() != null ? request.getNumberOfChildren() : 0;
        boolean singleTourType = request.getTourTypes().size() == 1;
        long nights = ChronoUnit.DAYS.between(request.getCheckInDate(), request.getCheckOutDate());
        if (nights <= 0) nights = 1;

        // ── Validate per-tourtype counts ──────────────────────────────────
        if (!singleTourType) {

            // Rule 1 — each individual selection cannot exceed global
            for (TourTypeSelectionRequest selection : request.getTourTypes()) {
                int selectionAdults   = selection.getNumberOfAdults()   != null ? selection.getNumberOfAdults()   : 0;
                int selectionChildren = selection.getNumberOfChildren() != null ? selection.getNumberOfChildren() : 0;

                if (selectionAdults > globalAdults) {
                    throw new RuntimeException(
                            "Tour type adults (" + selectionAdults + ") cannot exceed " +
                                    "the total group adults (" + globalAdults + ")"
                    );
                }
                if (selectionChildren > globalChildren) {
                    throw new RuntimeException(
                            "Tour type children (" + selectionChildren + ") cannot exceed " +
                                    "the total group children (" + globalChildren + ")"
                    );
                }
            }

            // Rule 2 — sum of all selections cannot be less than global
            int totalSelectionAdults = request.getTourTypes().stream()
                    .mapToInt(t -> t.getNumberOfAdults() != null ? t.getNumberOfAdults() : 0)
                    .sum();
            int totalSelectionChildren = request.getTourTypes().stream()
                    .mapToInt(t -> t.getNumberOfChildren() != null ? t.getNumberOfChildren() : 0)
                    .sum();

            if (totalSelectionAdults < globalAdults) {
                throw new RuntimeException(
                        "Total adults across all tour types (" + totalSelectionAdults + ") " +
                                "cannot be less than the group adults (" + globalAdults + "). " +
                                "Every person must be assigned to at least one tour type."
                );
            }
            if (totalSelectionChildren < globalChildren) {
                throw new RuntimeException(
                        "Total children across all tour types (" + totalSelectionChildren + ") " +
                                "cannot be less than the group children (" + globalChildren + "). " +
                                "Every child must be assigned to at least one tour type."
                );
            }
        }

        Reservation reservation = Reservation.builder()
                .user(user)
                .source(request.getSource())
                .checkInDate(request.getCheckInDate())
                .checkOutDate(request.getCheckOutDate())
                .groupName(request.getGroupName())
                .groupLeaderName(request.getGroupLeaderName())
                .demandeSpecial(request.getDemandeSpecial())
                .numberOfAdults(globalAdults)
                .numberOfChildren(globalChildren)
                .currency(request.getCurrency() != null ? request.getCurrency() : "USD")
                .promoCode(request.getPromoCode())
                .status(ReservationStatus.PENDING)
                .build();

        // ── Snapshot each TourType ────────────────────────────────────────
        double totalAmount = 0.0;

        for (TourTypeSelectionRequest selection : request.getTourTypes()) {
            TourType tourType = tourTypeRepository.findById(selection.getTourTypeId())
                    .orElseThrow(() -> new RuntimeException("TourType not found: " + selection.getTourTypeId()));

            // Single tour type → use global counts automatically
            // Multiple tour types → use per-selection counts (already validated above)
            int adults   = singleTourType
                    ? globalAdults
                    : (selection.getNumberOfAdults()   != null ? selection.getNumberOfAdults()   : 0);
            int children = singleTourType
                    ? globalChildren
                    : (selection.getNumberOfChildren() != null ? selection.getNumberOfChildren() : 0);

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
                    .numberOfNights((int) nights)   // ← store nights in snapshot too
                    .build();

            reservation.addTourType(snapshot);
            totalAmount += snapshot.getTotalPrice();
        }

        reservation.setTotalAmount(totalAmount);


        // ── Participants ──────────────────────────────────────────────────
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

        // ── Extras — snapshotted from catalog ────────────────────────────
// ── Extras ────────────────────────────────────────────────────
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
                        .totalPrice(catalog.getUnitPrice() * e.getQuantity())  // ← fix
                        .isActive(true)
                        .build();
                reservation.addExtra(extra);
            });
        }

// ← this now reads correct totalPrice from in-memory extras
        reservation.setTotalExtrasAmount(reservation.calculateTotalExtrasAmount());


        return reservationMapper.toResponse(reservationRepository.save(reservation));
    }
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

        // ✅ Block updates on terminal statuses
        ReservationStatus current = reservation.getStatus();

        if (current == ReservationStatus.COMPLETED) {
            throw new ReservationStatusException(
                    "This reservation is already completed and cannot be modified."
            );
        }
        if (current == ReservationStatus.CANCELLED) {
            throw new ReservationStatusException(
                    "This reservation has already been cancelled and cannot be modified."
            );
        }
        if (current == ReservationStatus.CHECKED_IN) {
            throw new ReservationStatusException(
                    "This reservation is currently checked-in and cannot be cancelled."
            );
        }
        if (current == ReservationStatus.REJECTED) {
            throw new ReservationStatusException(
                    "This reservation has been rejected and cannot be modified."
            );
        }

        // ✅ Role-based restrictions for PARTENAIRE and CLIENT
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdminOrCamping = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")
                        || a.getAuthority().equals("ROLE_CAMPING"));

        if (!isAdminOrCamping) {
            // Only CANCELLED is allowed
            if (status != ReservationStatus.CANCELLED) {
                throw new AccessDeniedException(
                        "You are not authorized to set this status. Only cancellation is allowed."
                );
            }

            // Must be more than 48H before check-in
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

        return reservationMapper.toResponse(reservationRepository.save(reservation));
    }

    @Override
    public ReservationResponse updateReservation(UUID reservationId, ReservationUpdateRequest request) {

        Reservation reservation = findById(reservationId);

        // ── 1. Ownership guard ────────────────────────────────────────
        // Since only CLIENT/PARTENAIRE reach this method (enforced by @PreAuthorize),
        // we always verify the reservation belongs to the authenticated user.
        Jwt jwt = (Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

// TEMPORARY — print all claims to find the right key
        System.out.println("=== JWT CLAIMS ===");
        jwt.getClaims().forEach((key, value) -> System.out.println(key + " : " + value));
        System.out.println("==================");

        String email = jwt.getClaim("email");

        User authenticatedUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));

        if (!reservation.getUser().getUserId().equals(authenticatedUser.getUserId())) {
            throw new AccessDeniedException("You can only edit your own reservations.");
        }

        // ── 2. Status guard ───────────────────────────────────────────
        // Only PENDING and CONFIRMED reservations can be edited.
        // CHECKED_IN, COMPLETED, CANCELLED, REJECTED are all frozen.
        if (reservation.getStatus() == ReservationStatus.CHECKED_IN  ||
                reservation.getStatus() == ReservationStatus.COMPLETED   ||
                reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new IllegalStateException(
                    "Cannot edit a reservation with status: " + reservation.getStatus()
            );
        }

        // ── 3. Simple fields (partial update — only set if provided) ──
        // Each field is only overwritten if the client actually sent it.
        if (request.getCheckInDate()     != null) reservation.setCheckInDate(request.getCheckInDate());
        if (request.getCheckOutDate()    != null) reservation.setCheckOutDate(request.getCheckOutDate());
        if (request.getGroupName()       != null) reservation.setGroupName(request.getGroupName());
        if (request.getGroupLeaderName() != null) reservation.setGroupLeaderName(request.getGroupLeaderName());
        if (request.getDemandeSpecial()  != null) reservation.setDemandeSpecial(request.getDemandeSpecial());
        if (request.getPromoCode()       != null) reservation.setPromoCode(request.getPromoCode());
        if (request.getCurrency()        != null) reservation.setCurrency(request.getCurrency());
        // update group size if provided
        if (request.getNumberOfAdults()   != null) reservation.setNumberOfAdults(request.getNumberOfAdults());
        if (request.getNumberOfChildren() != null) reservation.setNumberOfChildren(request.getNumberOfChildren());

        // ── 4. Status reset ───────────────────────────────────────────
        // If the reservation was already CONFIRMED and the user edits it,
        // it goes back to PENDING so the admin re-approves the new version.
        // If it was already PENDING it stays PENDING — no change needed.
        if (reservation.getStatus() == ReservationStatus.CONFIRMED ||
                reservation.getStatus() == ReservationStatus.REJECTED) {
            reservation.setStatus(ReservationStatus.PENDING);
            reservation.setRejectionReason(null); // clear the rejection reason on resubmit
        }

        // ── 5. Recalculate nights ─────────────────────────────────────
        // Must be done AFTER updating checkIn/checkOut so nights
        // reflect the new dates, not the old ones.
        long nights = ChronoUnit.DAYS.between(
                reservation.getCheckInDate(),
                reservation.getCheckOutDate()
        );
        if (nights <= 0) nights = 1;

        // Price tier depends on the user's role (partner gets partner prices)
        boolean isPartner = authenticatedUser.getRole() == UserRole.PARTENAIRE;

        // ── 6. Replace TourTypes (clear + rebuild) ────────────────────
        // We use a full replace strategy: clear all existing snapshots
        // and rebuild from the new selection sent by the client.
        // orphanRemoval=true on the entity ensures DB rows are deleted.
        // Prices are recalculated fresh from the catalog at update time.
        if (request.getTourTypes() != null && !request.getTourTypes().isEmpty()) {

            int globalAdults   = reservation.getNumberOfAdults();
            int globalChildren = reservation.getNumberOfChildren();
            boolean singleTourType = request.getTourTypes().size() == 1;

            // Same validation rules as creation
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
                        "Total adults across all tour types (" + totalSelAdults + ") " +
                                "cannot be less than group adults (" + globalAdults + ")."
                );
                if (totalSelChildren < globalChildren) throw new RuntimeException(
                        "Total children across all tour types (" + totalSelChildren + ") " +
                                "cannot be less than group children (" + globalChildren + ")."
                );
            }

            reservation.getTourTypes().clear();

            double totalAmount = 0.0;
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
                totalAmount += snapshot.getTotalPrice();
            }
            reservation.setTotalAmount(totalAmount);
        }

        // ── 7. Replace Participants (clear + rebuild) ─────────────────
        // If the client sends a participants list, replace entirely.
        // If null is sent, participants are left untouched.
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

        // ── 8. Replace Extras (clear + rebuild) ──────────────────────
        // Same strategy: full replace if the client sends the extras list.
        // Prices are always fetched fresh from the catalog.
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

        return reservationMapper.toResponse(reservationRepository.save(reservation));
    }

    @Override
    public void deleteReservation(UUID reservationId) {
        reservationRepository.delete(findById(reservationId));
    }

    private Reservation findById(UUID reservationId) {
        return reservationRepository.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("Reservation not found: " + reservationId));
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
}