package com.gescli.ProgrammationTravaux.dto;

import lombok.Data;

@Data
public class DevisRequestDTO {
    private String devisCode;
    private String dateCreation;
    private String documentId;
    private String statut;
    private String clientCode;
    private String demOption;
}