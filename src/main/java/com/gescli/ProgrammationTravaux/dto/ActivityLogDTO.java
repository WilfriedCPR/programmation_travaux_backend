package com.gescli.ProgrammationTravaux.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ActivityLogDTO {
    private String id;
    private String type;
    private String description;
    private String acteur;
    private String reference;
    private String devisId;
    private String agentId;
    private LocalDateTime dateCreation;
}
