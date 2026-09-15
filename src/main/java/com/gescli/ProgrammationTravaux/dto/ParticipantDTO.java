package com.gescli.ProgrammationTravaux.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParticipantDTO {
    private String id;
    @NotBlank(message = "Le nom du participant est obligatoire")
    private String nom;
    private String prenom;
    private String entreprise;
    private String contact;
    private boolean externe;
}
