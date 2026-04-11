package com.gescli.ProgrammationTravaux.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

import java.time.LocalDateTime;

@Entity
@Table(name = "travaux_contrainte")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class TravauxContrainte {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "date_mise_contrainte")
    private LocalDateTime dateMiseContrainte;

    @Column(name = "date_levee_contrainte")
    private LocalDateTime dateLeveeContrainte;

    @Column(name = "observation_mise")
    private String observationMise;

    @Column(name = "observation_levee")
    private String observationLevee;

    private boolean active;

    @ManyToOne
    @JoinColumn(name = "travaux_id", nullable = false)
    @NotFound(action = NotFoundAction.IGNORE)
    private Travaux travaux;

    @ManyToOne
    @JoinColumn(name = "planning_id")
    @NotFound(action = NotFoundAction.IGNORE)
    private PlanningTravaux planning;

    @ManyToOne
    @JoinColumn(name = "contrainte_id", nullable = false)
    @NotFound(action = NotFoundAction.IGNORE)
    private Contrainte contrainte;
}