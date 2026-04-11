package com.gescli.ProgrammationTravaux.mapper;

import com.gescli.ProgrammationTravaux.dto.MaterielDTO;
import com.gescli.ProgrammationTravaux.entity.Materiel;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MaterielMapper {
    MaterielDTO toDto(Materiel materiel);
    Materiel toEntity(MaterielDTO dto);
    void updateFromDto(MaterielDTO dto, @MappingTarget Materiel entity);
}
