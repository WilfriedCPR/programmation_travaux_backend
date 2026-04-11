package com.gescli.ProgrammationTravaux.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "type_travaux")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class TypeTravaux {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true)
    private String libelle;
}