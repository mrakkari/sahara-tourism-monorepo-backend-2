package com.camping.duneinsolite.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class TourTypeRequest {

    @NotBlank(message = "Name is required")
    private String name;

    private String description;
    private String duration;

    @NotNull(message = "Passenger adult price is required")
    @Positive(message = "Price must be positive")
    private Double passengerAdultPrice;

    @NotNull(message = "Passenger child price is required")
    @Positive(message = "Price must be positive")
    private Double passengerChildPrice;

    @NotNull(message = "Partner adult price is required")
    @Positive(message = "Price must be positive")
    private Double partnerAdultPrice;

    @NotNull(message = "Partner child price is required")
    @Positive(message = "Price must be positive")
    private Double partnerChildPrice;
}