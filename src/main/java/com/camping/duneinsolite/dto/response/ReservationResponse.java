package com.camping.duneinsolite.dto.response;

import com.camping.duneinsolite.model.enums.ReservationStatus;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class ReservationResponse {
    private UUID reservationId;
    private UUID userId;
    private String userName;
    private String source;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private String groupName;
    private String groupLeaderName;
    private Integer numberOfAdults;
    private Integer numberOfChildren;
    private ReservationStatus status;
    private String rejectionReason;
    private Double totalAmount;
    private String currency;
    private String promoCode;
    private String demandeSpecial;
    private List<ReservationTourTypeResponse> tourTypes;  // ← now snapshots
    private List<ParticipantResponse> participants;
    private List<ReservationExtraResponse> extras;
    private Double totalExtrasAmount;
    private LocalDateTime createdAt;
    private LocalDateTime deletedAt;
}