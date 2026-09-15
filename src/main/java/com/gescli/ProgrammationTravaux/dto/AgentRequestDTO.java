package com.gescli.ProgrammationTravaux.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AgentRequestDTO {
    @NotBlank(message = "Le nom est obligatoire")
    private String nom;
    @NotBlank(message = "Le prénom est obligatoire")
    private String prenom;
    @NotBlank(message = "Le code est obligatoire")
    private String code;
    // Conservé pour compatibilité API, mais ignoré : tout agent appartient à SONABEL.
    private String structureId;
    @NotBlank(message = "Le rôle est obligatoire")
    private String roleName;
    @Size(min = 6, message = "Le mot de passe doit contenir au moins 6 caractères")
    private String password;
}
