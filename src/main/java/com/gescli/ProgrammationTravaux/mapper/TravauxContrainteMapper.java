package com.gescli.ProgrammationTravaux.mapper;

import com.gescli.ProgrammationTravaux.dto.TravauxContrainteResponseDTO;
import com.gescli.ProgrammationTravaux.entity.TravauxContrainte;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TravauxContrainteMapper {

    @Mapping(target = "travauxId", source = "travaux.id")
    @Mapping(target = "travauxLibelle", source = "travaux.libelle")
    @Mapping(target = "planningId", source = "planning.id")
    @Mapping(target = "contrainteLibelle", source = "contrainte.libelle")
    TravauxContrainteResponseDTO toDto(TravauxContrainte entity);
}
