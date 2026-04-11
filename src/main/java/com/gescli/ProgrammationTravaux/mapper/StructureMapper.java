package com.gescli.ProgrammationTravaux.mapper;

import com.gescli.ProgrammationTravaux.dto.StructureDTO;
import com.gescli.ProgrammationTravaux.entity.Structure;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface StructureMapper {

    StructureDTO toDto(Structure entity);

    Structure toEntity(StructureDTO dto);
}
