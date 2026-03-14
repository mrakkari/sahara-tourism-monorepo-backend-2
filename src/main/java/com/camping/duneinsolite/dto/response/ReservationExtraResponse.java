package com.camping.duneinsolite.dto.response;

import lombok.Data;
import java.util.UUID;

@Data
public class ReservationExtraResponse {
    private UUID reservationExtraId;
    private UUID reservationId;
    private String name;
    private String description;
    private String duration;
    private Integer quantity;
    private Double unitPrice;
    private Double totalPrice;
    private Boolean isActive;

   // private Double totalAmount;
}