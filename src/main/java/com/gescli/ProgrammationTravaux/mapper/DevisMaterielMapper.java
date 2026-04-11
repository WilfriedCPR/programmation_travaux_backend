package com.gescli.ProgrammationTravaux.mapper;

import com.gescli.ProgrammationTravaux.dto.DevisMaterielDTO;
import com.gescli.ProgrammationTravaux.entity.DevisMateriel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface DevisMaterielMapper {

    @Mapping(target = "materielId", source = "materiel.id")
    @Mapping(target = "materielLibelle", source = "materiel.libelle")
    @Mapping(target = "materielPrice", source = "materiel.prix")
    @Mapping(target = "materielCode", source = "materiel.code")
    @Mapping(target = "devisId", source = "devis.id")
    DevisMaterielDTO toDto(DevisMateriel entity);

    @Mapping(target = "devis", ignore = true)
    @Mapping(target = "materiel", ignore = true)
    DevisMateriel toEntity(DevisMaterielDTO dto);
}
