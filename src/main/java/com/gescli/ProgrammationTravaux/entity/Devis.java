package com.gescli.ProgrammationTravaux.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "tr_devis")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@EqualsAndHashCode(exclude = {"affectations", "programmations", "devisMateriels", "demandesMateriel"})
@ToString(exclude = {"affectations", "programmations", "devisMateriels", "demandesMateriel"})
public class Devis {

    @Id
    @Column(name = "id")
    private String id;

    @Column(name = "devis_code", nullable = false, unique = true)
    private String devisCode;

    @Column(name = "devis_date", nullable = false)
    private LocalDateTime dateCreation;

    @Column(name = "devis_status", nullable = false)
    @Convert(converter = DevisStatusConverter.class)
    private DevisStatut statut;

    @Transient
    private LocalDateTime dateSuppression;

    @Transient
    private Document document;

    @OneToMany(mappedBy = "devis", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<AffectationDevis> affectations = new HashSet<>();

    @OneToMany(mappedBy = "devis", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<PlanningTravaux> programmations = new HashSet<>();

    @OneToMany(mappedBy = "devis", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<DevisMateriel> devisMateriels = new HashSet<>();

    @OneToMany(mappedBy = "devis", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<DemandeMateriel> demandesMateriel = new HashSet<>();

    @ManyToOne
    @JoinColumn(name = "demande_dossier_id")
    private DemandeDossier dossier;

    @PrePersist
    void onCreate() {
        if (this.id == null || this.id.isBlank()) this.id = java.util.UUID.randomUUID().toString();
        if (this.dateCreation == null) this.dateCreation = LocalDateTime.now();
        if (this.statut == null) this.statut = DevisStatut.EN_COURS;
    }
}