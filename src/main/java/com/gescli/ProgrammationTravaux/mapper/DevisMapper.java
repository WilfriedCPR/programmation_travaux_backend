package com.gescli.ProgrammationTravaux.mapper;

import com.gescli.ProgrammationTravaux.dto.*;
import com.gescli.ProgrammationTravaux.entity.Client;
import com.gescli.ProgrammationTravaux.entity.DemandeDossier;
import com.gescli.ProgrammationTravaux.entity.Devis;
import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE,
        uses = {AffectationMapper.class})
public interface DevisMapper {

    @Mapping(target = "statut", expression = "java(devis.getStatut() != null ? devis.getStatut().name() : null)")
    @Mapping(target = "documentId", source = "document.id")
    @Mapping(target = "documentType", expression = "java(devis.getDocument() != null && devis.getDocument().getTypeDocument() != null ? devis.getDocument().getTypeDocument().name() : null)")
    @Mapping(target = "document", ignore = true)
    @Mapping(target = "dossier", source = "dossier")
    @Mapping(target = "devisMateriels", ignore = true)
    @Mapping(target = "demandesMateriel", ignore = true)
    @Mapping(target = "affectations", ignore = true)
    @Mapping(target = "programmations", ignore = true)
    DevisResponseDTO toDto(Devis devis);

    @Mapping(target = "client", source = "client")
    DossierDTO toDossierDto(DemandeDossier dossier);

    ClientDTO toClientDto(Client client);
}
