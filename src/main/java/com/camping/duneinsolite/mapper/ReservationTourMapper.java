package com.camping.duneinsolite.mapper;

import com.camping.duneinsolite.dto.response.ReservationTourResponse;
import com.camping.duneinsolite.model.ReservationTour;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ReservationTourMapper {
    ReservationTourResponse toResponse(ReservationTour reservationTour);
}