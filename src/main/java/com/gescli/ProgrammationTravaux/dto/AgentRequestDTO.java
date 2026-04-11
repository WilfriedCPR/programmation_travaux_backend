package com.gescli.ProgrammationTravaux.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AgentRequestDTO {

    @NotBlank(message = "Le nom est obligatoire")
    private String nom;

    @NotBlank(message = "Le prénom est obligatoire")
    private String prenom;

    @NotBlank(message = "Le code est obligatoire")
    private String code;

    private String structureId;

    @NotBlank(message = "Le rôle est obligatoire")
    private String roleName;

    @NotBlank(message = "Le mot de passe est obligatoire")
    private String password;
}