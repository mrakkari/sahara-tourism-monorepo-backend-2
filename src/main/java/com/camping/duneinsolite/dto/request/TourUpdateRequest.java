package com.camping.duneinsolite.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TourUpdateRequest {

    @NotBlank(message = "Tour name is required")
    private String name;

    private String description;

    private String duration;

    @NotNull(message = "Passenger adult price is required")
    private Double passengerAdultPrice;

    @NotNull(message = "Passenger child price is required")
    private Double passengerChildPrice;

    @NotNull(message = "Partner adult price is required")
    private Double partnerAdultPrice;

    @NotNull(message = "Partner child price is required")
    private Double partnerChildPrice;

    private Boolean isActive;
}