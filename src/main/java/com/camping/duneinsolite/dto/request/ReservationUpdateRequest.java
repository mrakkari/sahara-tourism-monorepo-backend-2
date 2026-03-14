package com.camping.duneinsolite.dto.request;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class ReservationUpdateRequest {

    // ── Simple fields ──────────────────────────────────────
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private String groupName;
    private String groupLeaderName;
    private String demandeSpecial;
    private String promoCode;
    private String currency;
    private Integer numberOfAdults;
    private Integer numberOfChildren;


    // ── Replace lists entirely (send the full desired state) ──
    private List<TourTypeSelectionRequest> tourTypes;     // same DTO as create
    private List<ParticipantRequest> participants;         // same DTO as create
    private List<ReservationExtraRequest> extras;          // same DTO as create
}
