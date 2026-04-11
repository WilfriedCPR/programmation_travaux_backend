package com.gescli.ProgrammationTravaux.dto;

import lombok.Data;
import java.util.Set;

@Data
public class AgentResponseDTO {
    private String id;
    private String nom;
    private String prenom;
    private String code;
    private String structureId;
    private String structureLibelle;
    private Set<String> rolesLibelles;
}