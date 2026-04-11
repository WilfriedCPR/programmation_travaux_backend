package com.gescli.ProgrammationTravaux.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class DevisMateriel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private int quantity;

    @ManyToOne
    @JoinColumn(name = "devis_id")
    private Devis devis;

    @ManyToOne
    @JoinColumn(name = "materiel_id")
    private Materiel materiel;
}