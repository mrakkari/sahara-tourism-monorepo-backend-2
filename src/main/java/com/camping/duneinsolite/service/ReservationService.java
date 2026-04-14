package com.camping.duneinsolite.service;


import com.camping.duneinsolite.dto.request.ReservationRequest;
import com.camping.duneinsolite.dto.request.ReservationUpdateRequest;
import com.camping.duneinsolite.dto.response.ReservationResponse;
import com.camping.duneinsolite.model.enums.ReservationStatus;
import java.util.List;
import java.util.UUID;

public interface ReservationService {
    ReservationResponse createReservation(ReservationRequest request);
    ReservationResponse getReservationById(UUID reservationId);
    List<ReservationResponse> getAllReservations();
    List<ReservationResponse> getReservationsByUser(UUID userId);
    List<ReservationResponse> getReservationsByStatus(ReservationStatus status);
    ReservationResponse updateReservationStatus(UUID reservationId, ReservationStatus status, String rejectionReason);
    ReservationResponse updateReservation(UUID reservationId, ReservationUpdateRequest request);
    void deleteReservation(UUID reservationId);
    List<ReservationResponse> getMyReservations(UUID userId);
    List<ReservationResponse> searchReservationsByName(String name);
}
