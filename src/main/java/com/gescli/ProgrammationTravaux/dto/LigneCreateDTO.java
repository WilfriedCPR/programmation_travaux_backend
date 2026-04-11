package com.gescli.ProgrammationTravaux.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LigneCreateDTO {
    private String materielId;
    @NotNull @Min(1)
    private Integer quantiteSortie;
    private String uniteMesure;
}