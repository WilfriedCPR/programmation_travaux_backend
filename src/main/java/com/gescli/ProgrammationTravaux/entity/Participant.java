package com.gescli.ProgrammationTravaux.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class Participant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String nom;

    private String prenom;
    private String entreprise;
    private String contact;

    @Column(nullable = false)
    private boolean externe = true;

    @Column(name = "date_creation")
    private LocalDateTime dateCreation;

    @PrePersist
    void onCreate() {
        if (this.dateCreation == null) this.dateCreation = LocalDateTime.now();
    }
}