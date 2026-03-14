package com.camping.duneinsolite.mapper;

import com.camping.duneinsolite.dto.request.TourTypeRequest;
import com.camping.duneinsolite.dto.response.TourTypeResponse;
import com.camping.duneinsolite.model.TourType;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TourTypeMapper {
    TourTypeResponse toResponse(TourType tourType);
    TourType toEntity(TourTypeRequest request);
}