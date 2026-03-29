package com.camping.duneinsolite.controller;


import com.camping.duneinsolite.dto.request.ReservationRequest;
import com.camping.duneinsolite.dto.request.ReservationUpdateRequest;
import com.camping.duneinsolite.dto.response.ReservationResponse;
import com.camping.duneinsolite.model.enums.ReservationStatus;
import com.camping.duneinsolite.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;
// ajoute d'une extras de la 1 ere fois
    @PostMapping
    @PreAuthorize("hasAnyRole('CLIENT', 'PARTENAIRE','ADMIN', 'CAMPING')")
    public ResponseEntity<ReservationResponse> createReservation(@Valid @RequestBody ReservationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reservationService.createReservation(request));
    }

    @GetMapping("/{reservationId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReservationResponse> getReservationById(@PathVariable UUID reservationId) {
        return ResponseEntity.ok(reservationService.getReservationById(reservationId));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CAMPING', 'PARTENAIRE')")
    public ResponseEntity<List<ReservationResponse>> getAllReservations() {
        return ResponseEntity.ok(reservationService.getAllReservations());
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAMPING', 'CLIENT', 'PARTENAIRE')")
    public ResponseEntity<List<ReservationResponse>> getReservationsByUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(reservationService.getReservationsByUser(userId));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAMPING')")
    public ResponseEntity<List<ReservationResponse>> getReservationsByStatus(@PathVariable ReservationStatus status) {
        return ResponseEntity.ok(reservationService.getReservationsByStatus(status));
    }

    @PatchMapping("/{reservationId}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAMPING', 'PARTENAIRE', 'CLIENT')")
    public ResponseEntity<ReservationResponse> updateStatus(
            @PathVariable UUID reservationId,
            @RequestParam ReservationStatus status,
            @RequestParam(required = false) String rejectionReason) {
        return ResponseEntity.ok(reservationService.updateReservationStatus(reservationId, status, rejectionReason));
    }

    @PutMapping("/{reservationId}")
    @PreAuthorize("hasAnyRole('CLIENT', 'PARTENAIRE')")
    public ResponseEntity<ReservationResponse> updateReservation(
            @PathVariable UUID reservationId,
            @Valid @RequestBody ReservationUpdateRequest request) {
        return ResponseEntity.ok(reservationService.updateReservation(reservationId, request));
    }

    @DeleteMapping("/{reservationId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteReservation(@PathVariable UUID reservationId) {
        reservationService.deleteReservation(reservationId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/my-reservations")
    @PreAuthorize("hasAnyRole('CLIENT', 'PARTENAIRE')")
    public ResponseEntity<List<ReservationResponse>> getMyReservations() {
        String userId = SecurityContextHolder.getContext()
                .getAuthentication().getName(); // gets the userId from the JWT
        return ResponseEntity.ok(reservationService.getMyReservations(UUID.fromString(userId)));
    }
}
