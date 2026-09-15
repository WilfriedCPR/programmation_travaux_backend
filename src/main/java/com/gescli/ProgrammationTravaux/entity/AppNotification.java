package com.gescli.ProgrammationTravaux.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "app_notification")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class AppNotification {
    @Id
    private String id;

    @Column(name = "agent_id", nullable = false)
    private String agentId;

    @Column(name = "titre", nullable = false, length = 180)
    private String titre;

    @Column(name = "message", nullable = false, length = 1000)
    private String message;

    @Column(name = "type", nullable = false, length = 60)
    private String type;

    @Column(name = "lien", length = 500)
    private String lien;

    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation;

    @Column(name = "date_lecture")
    private LocalDateTime dateLecture;

    @PrePersist
    void onCreate() {
        if (id == null || id.isBlank()) id = UUID.randomUUID().toString();
        if (dateCreation == null) dateCreation = LocalDateTime.now();
    }
}
