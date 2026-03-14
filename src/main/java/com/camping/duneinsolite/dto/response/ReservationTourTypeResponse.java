package com.camping.duneinsolite.dto.response;

import lombok.Data;
import java.util.UUID;

@Data
public class ReservationTourTypeResponse {
    private UUID reservationTourTypeId;
    private String name;
    private String description;
    private String duration;
    private Double adultPrice;
    private Double childPrice;
    private Integer numberOfAdults;
    private Integer numberOfChildren;
    private Double totalPrice;
    private Integer numberOfNights;
}