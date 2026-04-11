package com.gescli.ProgrammationTravaux.mapper;

import com.gescli.ProgrammationTravaux.dto.TypeTravauxDTO;
import com.gescli.ProgrammationTravaux.entity.TypeTravaux;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TypeTravauxMapper {

    TypeTravauxDTO toDto(TypeTravaux entity);

    TypeTravaux toEntity(TypeTravauxDTO dto);
}
