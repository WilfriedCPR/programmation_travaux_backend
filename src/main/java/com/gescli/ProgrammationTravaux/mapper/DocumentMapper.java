package com.gescli.ProgrammationTravaux.mapper;

import com.gescli.ProgrammationTravaux.dto.DocumentDTO;
import com.gescli.ProgrammationTravaux.entity.Document;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface DocumentMapper {

    @Mapping(target = "typeDocument", expression = "java(entity.getTypeDocument() != null ? entity.getTypeDocument().name() : null)")
    @Mapping(target = "demandeMaterielId", source = "demandeMateriel.id")
    DocumentDTO toDto(Document entity);

    @Mapping(target = "typeDocument", expression = "java(dto.getTypeDocument() != null ? com.gescli.ProgrammationTravaux.entity.TypeDocument.valueOf(dto.getTypeDocument()) : null)")
    @Mapping(target = "demandeMateriel", ignore = true)
    @Mapping(target = "planning", ignore = true)
    Document toEntity(DocumentDTO dto);
}
