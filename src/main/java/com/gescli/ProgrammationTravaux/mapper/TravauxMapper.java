package com.gescli.ProgrammationTravaux.mapper;

import com.gescli.ProgrammationTravaux.dto.TravauxDTO;
import com.gescli.ProgrammationTravaux.entity.Travaux;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TravauxMapper {

    @Mapping(target = "typeTravauxLibelle", source = "typeTravaux.libelle")
    @Mapping(target = "typeTravauxId", source = "typeTravaux.id")
    TravauxDTO toDto(Travaux entity);
}
