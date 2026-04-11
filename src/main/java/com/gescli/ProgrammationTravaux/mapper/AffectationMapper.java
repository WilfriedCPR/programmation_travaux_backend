package com.gescli.ProgrammationTravaux.mapper;

import com.gescli.ProgrammationTravaux.dto.AffectationResponseDTO;
import com.gescli.ProgrammationTravaux.entity.AffectationDevis;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AffectationMapper {

    @Mapping(target = "agentId", source = "agent.id")
    @Mapping(target = "agentNom", source = "agent.nom")
    @Mapping(target = "agentPrenom", source = "agent.prenom")
    @Mapping(target = "devisId", source = "devis.id")
    @Mapping(target = "devisCode", source = "devis.devisCode")
    @Mapping(target = "devisDejaAffectes", ignore = true)
    AffectationResponseDTO toDto(AffectationDevis entity);
}
