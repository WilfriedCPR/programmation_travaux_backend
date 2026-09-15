package com.gescli.ProgrammationTravaux.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "push_subscription")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class PushSubscription {
    @Id
    private String id;

    @Column(name = "agent_id", nullable = false)
    private String agentId;

    @Column(nullable = false, unique = true, columnDefinition = "TEXT")
    private String endpoint;

    @Column(columnDefinition = "TEXT")
    private String p256dh;

    @Column(name = "auth_key", columnDefinition = "TEXT")
    private String auth;

    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation;

    @Column(name = "derniere_utilisation", nullable = false)
    private LocalDateTime derniereUtilisation;

    @Column(nullable = false)
    private boolean actif = true;

    @PrePersist
    void onCreate() {
        if (id == null || id.isBlank()) id = UUID.randomUUID().toString();
        if (dateCreation == null) dateCreation = LocalDateTime.now();
        if (derniereUtilisation == null) derniereUtilisation = LocalDateTime.now();
    }
}
