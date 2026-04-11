package com.gescli.ProgrammationTravaux.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class PlanningTravaux {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private LocalDateTime dateDebut;

    private LocalDateTime dateFin;

    @ManyToOne
    @JoinColumn(name = "devis_id", nullable = false)
    private Devis devis;

    @ManyToOne
    @JoinColumn(name = "travaux_id", nullable = false)
    private Travaux travaux;

    @ManyToOne
    @JoinColumn(name = "agent_id")
    private Agent agent;

    @ManyToMany
    @JoinTable(
        name = "planning_travaux_agent",
        joinColumns = @JoinColumn(name = "planning_id"),
        inverseJoinColumns = @JoinColumn(name = "agent_id")
    )
    private Set<Agent> participants = new HashSet<>();

    @ManyToMany
    @JoinTable(
        name = "planning_travaux_participant",
        joinColumns = @JoinColumn(name = "planning_id"),
        inverseJoinColumns = @JoinColumn(name = "participant_id")
    )
    private Set<Participant> participantsExternes = new HashSet<>();

    @Column(nullable = false)
    private boolean cloture = false;

    private LocalDateTime clotureDate;

    @Column(nullable = false)
    private boolean deleted = false;

    @Column(nullable = false)
    private boolean ht = false;

    private String demOption;
}