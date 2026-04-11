package com.gescli.ProgrammationTravaux.dto;

import lombok.Data;

@Data
public class DevisClientInfoDTO {
    private boolean found;
    private String cliCode;
    private String cliNom;
    private String cliPrenom;
    private String cliRaisonSocial;
    private String demOption;
    private String displayName;
}