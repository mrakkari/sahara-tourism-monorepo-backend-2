package com.camping.duneinsolite.mapper;

import com.camping.duneinsolite.dto.response.ReservationTourTypeResponse;
import com.camping.duneinsolite.model.ReservationTourType;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ReservationTourTypeMapper {

    @Mapping(target = "totalPrice", expression = "java(tourType.getTotalPrice())")
    ReservationTourTypeResponse toResponse(ReservationTourType tourType);
}