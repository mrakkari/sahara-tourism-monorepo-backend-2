package com.camping.duneinsolite.mapper;

import com.camping.duneinsolite.dto.response.ParticipantResponse;
import com.camping.duneinsolite.dto.response.ReservationResponse;
import com.camping.duneinsolite.model.Participant;
import com.camping.duneinsolite.model.Reservation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {ReservationTourTypeMapper.class, ReservationExtraMapper.class})
public interface ReservationMapper {

    @Mapping(source = "user.userId", target = "userId")
    @Mapping(source = "user.name", target = "userName")
    ReservationResponse toResponse(Reservation reservation);

    ParticipantResponse toParticipantResponse(Participant participant);
}