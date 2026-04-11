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
    private String agentId;
    private String agentNom;
    private String agentPrenom;
    private String devisId;
    private String devisCode;
    private List<String> devisDejaAffectes;
}