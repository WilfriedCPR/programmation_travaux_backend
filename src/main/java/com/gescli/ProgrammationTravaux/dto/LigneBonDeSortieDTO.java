package com.gescli.ProgrammationTravaux.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LigneBonDeSortieDTO {
    private String code;
    private String designation;
    private Integer quantite;
    private String unite;
}