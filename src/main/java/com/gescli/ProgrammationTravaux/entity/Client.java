package com.gescli.ProgrammationTravaux.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tr_client")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class Client {

    @Id
    @Column(name = "id", nullable = false)
    private String id;

    @Column(name = "cli_code", nullable = false, unique = true)
    private String cliCode;

    @Column(name = "cli_nom")
    private String cliNom;

    @Column(name = "cli_prenom")
    private String cliPrenom;

    @Column(name = "cli_raison_sociale")
    private String cliRaisonSocial;
}