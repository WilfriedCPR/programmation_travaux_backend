package com.gescli.ProgrammationTravaux.mapper;

import com.gescli.ProgrammationTravaux.dto.DemandeMaterielDTO;
import com.gescli.ProgrammationTravaux.dto.LigneDemandeDTO;
import com.gescli.ProgrammationTravaux.entity.DemandeMateriel;
import com.gescli.ProgrammationTravaux.entity.DemandeMaterielLigne;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface DemandeMaterielMapper {

    @Mapping(target = "devisId", source = "devis.id")
    @Mapping(target = "bonDeSortieId", source = "bonDeSortie.id")
    @Mapping(target = "referenceId", ignore = true)
    @Mapping(target = "referenceType", ignore = true)
    DemandeMaterielDTO toDto(DemandeMateriel entity);

    @Mapping(target = "materielId",      source = "materiel.id")
    @Mapping(target = "materielCode",     source = "materiel.code")
    @Mapping(target = "materielLibelle",  source = "materiel.libelle")
    @Mapping(target = "materielUnite",    source = "materiel.unite")
    LigneDemandeDTO ligneToDto(DemandeMaterielLigne ligne);
}
