package com.gescli.ProgrammationTravaux.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class DevisResponseDTO {
    private String id;
    private String devisCode;
    private String documentId;
    private String documentType;
    private DocumentDTO document;
    private LocalDateTime dateCreation;
    private LocalDateTime dateSuppression;
    private String statut;
    private DossierDTO dossier;
    private List<DevisMaterielDTO> devisMateriels;
    private List<DemandeMaterielDTO> demandesMateriel;
    private List<AffectationResponseDTO> affectations;
    private List<PlanningTravauxResponseDTO> programmations;
}