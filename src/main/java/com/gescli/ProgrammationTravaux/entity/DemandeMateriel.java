package com.gescli.ProgrammationTravaux.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class DemandeMateriel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private LocalDateTime dateValidation;
    private boolean valide;
    private LocalDateTime dateDemande;
    private String referenceValue;

    @Column(nullable = false)
    private boolean deleted = false;

    @ManyToOne
    @JoinColumn(name = "devis_id")
    private Devis devis;

    @OneToOne
    @JoinColumn(name = "document_id")
    private Document bonDeSortie;

    @OneToMany(mappedBy = "demande", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DemandeMaterielLigne> lignes = new ArrayList<>();
}