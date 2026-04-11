package com.gescli.ProgrammationTravaux.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "materiel")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class Materiel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String libelle;
    private String designation;
    @Column(name = "price")
    private double prix;
    private String code;
    private String unite;
}