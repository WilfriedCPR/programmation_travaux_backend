package com.gescli.ProgrammationTravaux.dto;

import com.gescli.ProgrammationTravaux.entity.Structure;
import lombok.Data;

@Data
public class StructureDTO {
    private String id;
    private String libelle;

    public static StructureDTO fromEntity(Structure s) {
        if (s == null) return null;
        StructureDTO dto = new StructureDTO();
        dto.setId(s.getId());
        dto.setLibelle(s.getLibelle());
        return dto;
    }
}