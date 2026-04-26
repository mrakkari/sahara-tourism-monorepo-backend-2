package com.camping.duneinsolite.mapper;

import com.camping.duneinsolite.dto.response.ParticipantResponse;
import com.camping.duneinsolite.dto.response.ReservationResponse;
import com.camping.duneinsolite.model.Participant;
import com.camping.duneinsolite.model.Reservation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {
        ReservationTourTypeMapper.class,
        ReservationExtraMapper.class,
        ReservationTourMapper.class,
        SourceMapper.class,
        GuideMapper.class,
        ChauffeurMapper.class
})
public interface ReservationMapper {

    @Mapping(source = "user.userId",   target = "userId")
    @Mapping(source = "user.name",     target = "userName")
    @Mapping(source = "sourceRef",     target = "source")
    @Mapping(source = "guides",        target = "guides")
    @Mapping(source = "chauffeurs",    target = "chauffeurs")
    ReservationResponse toResponse(Reservation reservation);

    ParticipantResponse toParticipantResponse(Participant participant);
}