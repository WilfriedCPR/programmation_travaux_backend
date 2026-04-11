package com.gescli.ProgrammationTravaux.mapper;

import com.gescli.ProgrammationTravaux.dto.ParticipantDTO;
import com.gescli.ProgrammationTravaux.entity.Participant;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ParticipantMapper {

    ParticipantDTO toDto(Participant entity);

    Participant toEntity(ParticipantDTO dto);

    void updateFromDto(ParticipantDTO dto, @MappingTarget Participant entity);
}
