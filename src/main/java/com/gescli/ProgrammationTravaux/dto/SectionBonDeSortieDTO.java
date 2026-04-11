package com.gescli.ProgrammationTravaux.dto;

import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SectionBonDeSortieDTO {
    private String titreSection;
    private List<LigneBonDeSortieDTO> lignesMateriel;
}