package com.camping.duneinsolite.dto.request;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.UUID;
@Data
public class TourTypeSelectionRequest {
    @NotNull(message = "Tour type ID is required")
    private UUID tourTypeId;

    @Min(value = 0, message = "Number of adults cannot be negative")
    private Integer numberOfAdults;

    @Min(value = 0, message = "Number of children cannot be negative")
    private Integer numberOfChildren;
}
