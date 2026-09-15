package com.gescli.ProgrammationTravaux.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@EqualsAndHashCode(exclude = {"agent", "devis"})
@ToString(exclude = {"agent", "devis"})
public class AffectationDevis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private LocalDateTime dateAffectation;

    private LocalDateTime dateRetraitAffectation;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "affecte_par")
    private String affectePar;

    @Column(name = "retire_par")
    private String retirePar;

    @ManyToOne(optional = false)
    @JoinColumn(name = "agent_id")
    private Agent agent;

    @ManyToOne(optional = false)
    @JoinColumn(name = "devis_id")
    private Devis devis;
}