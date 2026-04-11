package com.gescli.ProgrammationTravaux.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class DemandeMaterielLigne {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "demande_id")
    private DemandeMateriel demande;

    @ManyToOne(optional = false)
    @JoinColumn(name = "materiel_id")
    private Materiel materiel;

    private int quantite;
    private String reference;
}