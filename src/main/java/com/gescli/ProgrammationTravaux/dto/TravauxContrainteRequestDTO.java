package com.gescli.ProgrammationTravaux.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TravauxContrainteRequestDTO {
    private String travauxId;
    private String contrainteLibelle;
    private LocalDateTime dateMiseContrainte;
    private LocalDateTime dateLeveeContrainte;
    private String observationMise;
    private String observationLevee;
    private boolean active;
}