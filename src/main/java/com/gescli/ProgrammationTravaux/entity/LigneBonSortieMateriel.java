package com.gescli.ProgrammationTravaux.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class LigneBonSortieMateriel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private Integer quantiteSortie;
    private String uniteMesure;

    @ManyToOne
    @JoinColumn(name = "materiel_id")
    private Materiel materiel;

    @ManyToOne
    @JoinColumn(name = "bon_sortie_id", nullable = false)
    private BonSortieMateriel bon;
}