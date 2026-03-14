package com.camping.duneinsolite.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class ExtraRequest {

    @NotBlank(message = "Name is required")
    private String name;

    private String description;
    private String duration;

    @NotNull(message = "Unit price is required")
    @Positive(message = "Unit price must be positive")
    private Double unitPrice;

    private Boolean isActive = true;
}