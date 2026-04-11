package com.gescli.ProgrammationTravaux.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TravauxDTO {
    private String id;
    private String typeTravauxId;
    private String typeTravauxLibelle;
    private String libelle;
}