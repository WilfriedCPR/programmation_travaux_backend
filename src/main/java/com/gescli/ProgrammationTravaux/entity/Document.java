package com.gescli.ProgrammationTravaux.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Getter @Setter
@NoArgsConstructor
@EqualsAndHashCode(exclude = {"demandeMateriel", "planning"})
@ToString(exclude = {"demandeMateriel", "planning"})
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String numeroDocument;
    private LocalDate dateDocument;
    private String client;
    private String site;
    private String codeProjet;
    private String codeClient;

    @Enumerated(EnumType.STRING)
    private TypeDocument typeDocument;

    @OneToOne(mappedBy = "bonDeSortie", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private DemandeMateriel demandeMateriel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "planning_id")
    private PlanningTravaux planning;

    private String participantsJson;
    private String observation;
    private String fileName;
    private String filePath;
    private String mimeType;

    @Enumerated(EnumType.STRING)
    private PvKind pvKind;
}