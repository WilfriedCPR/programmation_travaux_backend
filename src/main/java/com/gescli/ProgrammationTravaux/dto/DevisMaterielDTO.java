package com.gescli.ProgrammationTravaux.dto;

import lombok.Data;

@Data
public class DevisMaterielDTO {
    private String id;
    private int quantity;
    private String materielId;
    private String materielLibelle;
    private double materielPrice;
    private String materielCode;
    private String devisId;
}