package com.gescli.ProgrammationTravaux.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tr_demande_dossier")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class DemandeDossier {

    @Id
    @Column(name = "id", nullable = false)
    private String id;

    @Column(name = "dem_option")
    private String demOption;

    @ManyToOne
    @JoinColumn(name = "tr_client_id", referencedColumnName = "id")
    private Client client;
}