package com.gescli.ProgrammationTravaux.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@EqualsAndHashCode(exclude = "agents")
@ToString(exclude = "agents")
public class Structure {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String libelle;

    @OneToMany(mappedBy = "structure")
    private Set<Agent> agents = new HashSet<>();
}