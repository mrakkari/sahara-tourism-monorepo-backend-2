package com.camping.duneinsolite.service.impl;

import com.camping.duneinsolite.dto.request.ReservationExtraRequest;
import com.camping.duneinsolite.dto.response.ReservationExtraResponse;
import com.camping.duneinsolite.dto.response.ReservationExtrasListResponse;
import com.camping.duneinsolite.mapper.ReservationExtraMapper;
import com.camping.duneinsolite.model.Extra;
import com.camping.duneinsolite.model.Reservation;
import com.camping.duneinsolite.model.ReservationExtra;
import com.camping.duneinsolite.repository.ExtraRepository;
import com.camping.duneinsolite.repository.ReservationExtraRepository;
import com.camping.duneinsolite.repository.ReservationRepository;
import com.camping.duneinsolite.service.ReservationExtraService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ReservationExtraServiceImpl implements ReservationExtraService {

    private final ReservationExtraRepository reservationExtraRepository;
    private final ReservationRepository reservationRepository;
    private final ExtraRepository extraRepository;          // ← catalog
    private final ReservationExtraMapper reservationExtraMapper;

    /**
     * Scenario 2 — Client adds an extra to an already existing reservation.
     * Looks up catalog by extraId, snapshots name/description/duration/unitPrice.
     */
    @Override
    public ReservationExtraResponse createExtra(ReservationExtraRequest request) {
        if (request.getReservationId() == null) {
            throw new RuntimeException("reservationId is required when adding an extra to an existing reservation");
        }

        Reservation reservation = reservationRepository.findById(request.getReservationId())
                .orElseThrow(() -> new RuntimeException("Reservation not found: " + request.getReservationId()));

        Extra catalog = extraRepository.findById(request.getExtraId())
                .orElseThrow(() -> new RuntimeException("Extra not found in catalog: " + request.getExtraId()));

        // Compute total price for this extra
        double totalExtraPrice = catalog.getUnitPrice() * request.getQuantity();

        ReservationExtra extra = ReservationExtra.builder()
                .reservation(reservation)
                .name(catalog.getName())
                .description(catalog.getDescription())
                .duration(catalog.getDuration())
                .quantity(request.getQuantity())
                .unitPrice(catalog.getUnitPrice())
                .totalPrice(totalExtraPrice)
                .isActive(true)
                .build();

        ReservationExtra saved = reservationExtraRepository.save(extra);

        // Update totalExtrasAmount on the reservation
        double currentExtrasTotal = reservation.getTotalExtrasAmount() != null
                ? reservation.getTotalExtrasAmount() : 0.0;
        reservation.setTotalExtrasAmount(currentExtrasTotal + totalExtraPrice);
        reservationRepository.save(reservation);

        return reservationExtraMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ReservationExtraResponse getExtraById(UUID extraId) {
        return reservationExtraMapper.toResponse(findById(extraId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReservationExtraResponse> getAllExtras() {
        return reservationExtraRepository.findAll().stream()
                .map(reservationExtraMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ReservationExtrasListResponse getExtrasByReservation(UUID reservationId) {
        List<ReservationExtraResponse> extras = reservationExtraRepository
                .findByReservationReservationId(reservationId).stream()
                .map(reservationExtraMapper::toResponse)
                .toList();

        double totalExtrasAmount = extras.stream()
                .mapToDouble(e -> e.getTotalPrice() != null ? e.getTotalPrice() : 0.0)
                .sum();

        return new ReservationExtrasListResponse(extras, totalExtrasAmount);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReservationExtraResponse> getActiveExtras() {
        return reservationExtraRepository.findByIsActiveTrue().stream()
                .map(reservationExtraMapper::toResponse).toList();
    }

    /**
     * Only quantity is updatable after booking — name/price are snapshots.
     */
    @Override
    public ReservationExtraResponse updateExtra(UUID extraId, ReservationExtraRequest request) {
        ReservationExtra extra = findById(extraId);
        extra.setQuantity(request.getQuantity());
        return reservationExtraMapper.toResponse(reservationExtraRepository.save(extra));
    }

    @Override
    public void deleteExtra(UUID extraId) {
        ReservationExtra extra = findById(extraId);

        // Update reservation's totalExtrasAmount before deleting
        Reservation reservation = extra.getReservation();
        double currentTotal = reservation.getTotalExtrasAmount() != null
                ? reservation.getTotalExtrasAmount() : 0.0;
        reservation.setTotalExtrasAmount(
                Math.max(0.0, currentTotal - extra.getTotalPrice())
        );
        reservationRepository.save(reservation);
        reservationExtraRepository.delete(extra);
    }

    private ReservationExtra findById(UUID extraId) {
        return reservationExtraRepository.findById(extraId)
                .orElseThrow(() -> new RuntimeException("ReservationExtra not found: " + extraId));
    }
}