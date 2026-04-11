package com.gescli.ProgrammationTravaux.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlanningTravauxRequestDTO {
    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;
    private String devisId;
    private String travauxId;
    private String travauxName;
    private String demOption;
    private String agentId;
    private Boolean cloture;
    private List<String> participantIds;
    private List<String> participantNames;
    private String participantsExternes;
}