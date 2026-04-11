package com.gescli.ProgrammationTravaux.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParticipantDTO {
    private String id;
    private String nom;
    private String prenom;
    private String entreprise;
    private String contact;
    private boolean externe;
}