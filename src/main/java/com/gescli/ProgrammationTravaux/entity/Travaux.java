package com.gescli.ProgrammationTravaux.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "travaux")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@EqualsAndHashCode(exclude = {"programmations", "travauxContraintes"})
@ToString(exclude = {"programmations", "travauxContraintes"})
public class Travaux {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String libelle;

    @ManyToOne
    @JoinColumn(name = "type_travaux_id")
    private TypeTravaux typeTravaux;

    @OneToMany(mappedBy = "travaux")
    private Set<PlanningTravaux> programmations = new HashSet<>();

    @OneToMany(mappedBy = "travaux")
    private Set<TravauxContrainte> travauxContraintes = new HashSet<>();
}