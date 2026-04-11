package com.gescli.ProgrammationTravaux.dto;

import lombok.Data;

@Data
public class MaterielDTO {
    private String id;
    private String libelle;
    private String designation;
    private double prix;
    private String code;
    private String unite;
}