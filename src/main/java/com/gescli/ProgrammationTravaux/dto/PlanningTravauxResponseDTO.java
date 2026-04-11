package com.gescli.ProgrammationTravaux.dto;

import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlanningTravauxResponseDTO {
    private String id;
    private String dateDebut;
    private String dateFin;
    private String devisId;
    private String devisCode;
    private String travauxId;
    private String travauxLibelle;
    private String typeTravauxLibelle;
    private String agentId;
    private String agentNomComplet;
    private boolean cloture;
    private String clotureDate;
    private List<AgentLiteDTO> participants;
    private String participantsExternes;
    private List<ParticipantDTO> participantsExternesList;
    private Boolean isHt;
    private String demOption;
}