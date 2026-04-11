package com.gescli.ProgrammationTravaux.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class BonSortieMateriel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true)
    private String numeroBon;

    @Column(nullable = false)
    private String numeroDevis;

    private String clientCode;
    private String clientNom;
    private String ville;
    private String referenceCommande;
    private String motifOperation;

    @Column(nullable = false)
    private LocalDateTime dateSortie;

    @ManyToOne
    @JoinColumn(name = "demande_materiel_id")
    private DemandeMateriel demandeMateriel;

    @ManyToOne
    @JoinColumn(name = "agent_validateur_id")
    private Agent agentValidateur;

    @ManyToOne
    @JoinColumn(name = "bon_initial_id")
    private BonSortieMateriel bonInitial;

    @OneToMany(mappedBy = "bon", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<LigneBonSortieMateriel> lignes = new HashSet<>();

    public void addLigne(LigneBonSortieMateriel ligne) {
        this.lignes.add(ligne);
        ligne.setBon(this);
    }
}