package com.gescli.ProgrammationTravaux.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class AffectationResponseDTO {
    private String id;
    private LocalDateTime dateAffectation;
    private LocalDateTime dateRetraitAffectation;
    private boolean active;
    private String affectePar;
    private String retirePar;
    private String agentId;
    private String agentNom;
    private String agentPrenom;
    private String agentCode;
    private String structureId;
    private String structureLibelle;
    private String devisId;
    private String devisCode;
    private String devisStatut;
    private String clientLibelle;
    private java.time.LocalDateTime planningDateDebut;
    private java.time.LocalDateTime planningDateFin;
    private java.util.Set<String> agentRoles;
    private boolean planningConflict;
    private java.util.List<String> conflictMessages;
    private List<String> devisDejaAffectes;
}