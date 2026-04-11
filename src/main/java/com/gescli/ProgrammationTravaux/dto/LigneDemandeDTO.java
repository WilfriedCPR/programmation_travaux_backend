package com.gescli.ProgrammationTravaux.dto;

import lombok.Data;

@Data
public class LigneDemandeDTO {
    private String materielId;
    private String materielCode;
    private String materielLibelle;
    private String materielUnite;
    private int quantite;
    private String reference;
}