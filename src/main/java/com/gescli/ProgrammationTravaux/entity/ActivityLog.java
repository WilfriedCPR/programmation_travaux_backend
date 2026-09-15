package com.gescli.ProgrammationTravaux.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "activity_log")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class ActivityLog {
    @Id
    private String id;

    @Column(nullable = false, length = 80)
    private String type;

    @Column(nullable = false, length = 1200)
    private String description;

    private String acteur;
    private String reference;

    @Column(name = "devis_id")
    private String devisId;

    @Column(name = "agent_id")
    private String agentId;

    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation;

    @PrePersist
    void onCreate() {
        if (id == null || id.isBlank()) id = UUID.randomUUID().toString();
        if (dateCreation == null) dateCreation = LocalDateTime.now();
    }
}
