package com.gescli.ProgrammationTravaux.dto;

import com.gescli.ProgrammationTravaux.entity.Structure;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class StructureDTO {
    private String id;

    @NotBlank(message = "Le libellé de la structure est obligatoire")
    private String libelle;

    public static StructureDTO fromEntity(Structure s) {
        if (s == null) return null;
        StructureDTO dto = new StructureDTO();
        dto.setId(s.getId());
        dto.setLibelle(s.getLibelle());
        return dto;
    }
}
