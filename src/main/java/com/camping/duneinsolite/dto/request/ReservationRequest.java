package com.camping.duneinsolite.dto.request;


import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
public class ReservationRequest {

    @NotNull(message = "User ID is required")
    private UUID userId;

    private String source;

    @NotNull(message = "Check-in date is required")
    private LocalDate checkInDate;

    @NotNull(message = "Check-out date is required")
    private LocalDate checkOutDate;

    private String groupName;
    private String groupLeaderName;

    @NotNull(message = "Number of adults is required")
    @Min(value = 0, message = "Number of adults cannot be negative")
    private Integer numberOfAdults;

    @NotNull(message = "Number of children is required")
    @Min(value = 0, message = "Number of children cannot be negative")
    private Integer numberOfChildren;

    private String currency;
    private String promoCode;
    private String demandeSpecial;

    @NotEmpty(message = "At least one tour type is required")
    private List<TourTypeSelectionRequest> tourTypes;

    private List<ParticipantRequest> participants;
    private List<ReservationExtraRequest> extras;
}
