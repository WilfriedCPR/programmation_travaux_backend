package com.gescli.ProgrammationTravaux.dto;

import jakarta.validation.Valid;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BonDeSortieMaterielDTO {
    private String id;
    private String numeroBon;
    private String numeroDevis;
    private String demandeMaterielId;
    private String agentValidateurId;
    private String bonInitialId;
    private String codeClient;
    private String clientNomComplet;
    private String ville;
    private String motifOperation;
    private String referenceCommande;
    private LocalDateTime dateSortie;
    private List<SectionBonDeSortieDTO> sections;
    @Valid
    private List<LigneCreateDTO> lignes;
}