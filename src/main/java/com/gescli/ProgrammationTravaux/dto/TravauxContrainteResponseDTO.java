package com.gescli.ProgrammationTravaux.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TravauxContrainteResponseDTO {
    private String id;
    private String travauxId;
    private String planningId;
    private String contrainteLibelle;
    private String travauxLibelle;
    private LocalDateTime dateMiseContrainte;
    private LocalDateTime dateLeveeContrainte;
    private String observationMise;
    private String observationLevee;
    private boolean active;
}