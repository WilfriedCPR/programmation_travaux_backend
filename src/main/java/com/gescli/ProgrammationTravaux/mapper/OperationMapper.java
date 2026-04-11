package com.gescli.ProgrammationTravaux.mapper;

import java.util.List;

import com.gescli.ProgrammationTravaux.dto.OperationDTO;
import com.gescli.ProgrammationTravaux.entity.Operation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface OperationMapper {

    @Mapping(target = "typeOperation", expression = "java(entity.getTypeOperation() != null ? entity.getTypeOperation().name() : null)")
    @Mapping(source = "agent.id", target = "agentId")
    @Mapping(source = "demandeMateriel.id", target = "demandeMaterielId")
    OperationDTO toDto(Operation entity);

    List<OperationDTO> toDTOs(List<Operation> entities);

    @Mapping(target = "agent", ignore = true)
    @Mapping(target = "demandeMateriel", ignore = true)
    @Mapping(target = "typeOperation", expression = "java(dto.getTypeOperation() != null ? com.gescli.ProgrammationTravaux.entity.TypeOperation.valueOf(dto.getTypeOperation()) : null)")
    Operation toEntity(OperationDTO dto);
}
