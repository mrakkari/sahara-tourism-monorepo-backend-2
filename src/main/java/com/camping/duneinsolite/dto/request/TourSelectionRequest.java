package com.camping.duneinsolite.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class TourSelectionRequest {

    @NotNull(message = "Tour ID is required")
    private UUID tourId;

}