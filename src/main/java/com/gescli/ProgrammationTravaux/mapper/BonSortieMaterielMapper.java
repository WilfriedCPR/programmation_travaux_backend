package com.gescli.ProgrammationTravaux.mapper;

import com.gescli.ProgrammationTravaux.dto.BonDeSortieMaterielDTO;
import com.gescli.ProgrammationTravaux.entity.BonSortieMateriel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface BonSortieMaterielMapper {

    @Mapping(target = "demandeMaterielId", source = "demandeMateriel.id")
    @Mapping(target = "agentValidateurId", source = "agentValidateur.id")
    @Mapping(target = "bonInitialId", source = "bonInitial.id")
    @Mapping(target = "codeClient", source = "clientCode")
    @Mapping(target = "clientNomComplet", source = "clientNom")
    @Mapping(target = "lignes", ignore = true)
    @Mapping(target = "sections", ignore = true)
    BonDeSortieMaterielDTO toDto(BonSortieMateriel entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "demandeMateriel", ignore = true)
    @Mapping(target = "agentValidateur", ignore = true)
    @Mapping(target = "bonInitial", ignore = true)
    @Mapping(target = "lignes", ignore = true)
    @Mapping(target = "dateSortie", ignore = true)
    @Mapping(target = "clientCode", source = "codeClient")
    @Mapping(target = "clientNom", source = "clientNomComplet")
    BonSortieMateriel toEntity(BonDeSortieMaterielDTO dto);

    List<BonDeSortieMaterielDTO> toDTOs(List<BonSortieMateriel> entities);
}
