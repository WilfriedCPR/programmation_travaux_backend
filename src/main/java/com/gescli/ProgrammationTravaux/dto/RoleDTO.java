package com.gescli.ProgrammationTravaux.dto;

import com.gescli.ProgrammationTravaux.entity.Role;
import lombok.Data;

@Data
public class RoleDTO {
    private String id;
    private String libelle;

    public static RoleDTO fromEntity(Role role) {
        if (role == null) return null;
        RoleDTO dto = new RoleDTO();
        dto.setId(role.getId());
        dto.setLibelle(role.getLibelle());
        return dto;
    }
}