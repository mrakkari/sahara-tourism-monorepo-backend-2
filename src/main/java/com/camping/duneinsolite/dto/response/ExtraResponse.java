package com.camping.duneinsolite.dto.response;

import lombok.Data;
import java.util.UUID;

@Data
public class ExtraResponse {
    private UUID extraId;
    private String name;
    private String description;
    private String duration;
    private Double unitPrice;
    private Boolean isActive;
}