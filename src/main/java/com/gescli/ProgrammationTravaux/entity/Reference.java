package com.gescli.ProgrammationTravaux.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "reference")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class Reference {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeReference typeReference;

    @Column(nullable = false)
    private String reference;
}
