package com.gescli.ProgrammationTravaux.mapper;

import com.gescli.ProgrammationTravaux.dto.AgentRequestDTO;
import com.gescli.ProgrammationTravaux.dto.AgentResponseDTO;
import com.gescli.ProgrammationTravaux.entity.Agent;
import org.mapstruct.*;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AgentMapper {

    @Mapping(target = "structureId", source = "structure.id")
    @Mapping(target = "structureLibelle", source = "structure.libelle")
    @Mapping(target = "rolesLibelles", expression = "java(agent.getRoles().stream().map(r -> r.getLibelle()).collect(java.util.stream.Collectors.toSet()))")
    @Mapping(target = "activeAffectationsCount", expression = "java(agent.getAffectations() == null ? 0 : (int) agent.getAffectations().stream().filter(com.gescli.ProgrammationTravaux.entity.AffectationDevis::isActive).count())")
    AgentResponseDTO toDto(Agent agent);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "structure", ignore = true)
    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "affectations", ignore = true)
    @Mapping(target = "programmations", ignore = true)
    Agent toEntity(AgentRequestDTO dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "structure", ignore = true)
    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "affectations", ignore = true)
    @Mapping(target = "programmations", ignore = true)
    void updateAgentFromDto(AgentRequestDTO dto, @MappingTarget Agent agent);
}
