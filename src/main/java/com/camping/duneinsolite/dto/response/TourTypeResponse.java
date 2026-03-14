package com.camping.duneinsolite.dto.response;

import lombok.Data;
import java.util.UUID;

@Data
public class TourTypeResponse {
    private UUID tourTypeId;
    private String name;
    private String description;
    private String duration;
    private Double passengerAdultPrice;
    private Double passengerChildPrice;
    private Double partnerAdultPrice;
    private Double partnerChildPrice;
}