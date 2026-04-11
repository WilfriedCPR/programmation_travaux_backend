package com.gescli.ProgrammationTravaux.mapper;

import java.util.List;

import com.gescli.ProgrammationTravaux.dto.ReferenceDTO;
import com.gescli.ProgrammationTravaux.entity.Reference;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ReferenceMapper {

    @Mapping(target = "typeReference", expression = "java(entity.getTypeReference() != null ? entity.getTypeReference().name() : null)")
    ReferenceDTO toDto(Reference entity);

    List<ReferenceDTO> toDTOs(List<Reference> entities);

    @Mapping(target = "typeReference", expression = "java(dto.getTypeReference() != null ? com.gescli.ProgrammationTravaux.entity.TypeReference.valueOf(dto.getTypeReference()) : null)")
    Reference toEntity(ReferenceDTO dto);
}
