package com.gescli.ProgrammationTravaux.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "operation")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class Operation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Enumerated(EnumType.STRING)
    private TypeOperation typeOperation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "demande_materiel_id")
    private DemandeMateriel demandeMateriel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id")
    private Agent agent;
}
