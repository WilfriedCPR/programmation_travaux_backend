package com.gescli.ProgrammationTravaux.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@EqualsAndHashCode(exclude = {"affectations", "programmations"})
@ToString(exclude = {"affectations", "programmations"})
public class Agent {

    @Id
    private String id;

    private String nom;
    private String prenom;
    private String code;

    @ManyToOne
    @JoinColumn(name = "structure_id")
    private Structure structure;

    @ManyToMany
    @JoinTable(
        name = "agent_role",
        joinColumns = @JoinColumn(name = "agent_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    @OneToMany(mappedBy = "agent")
    private Set<AffectationDevis> affectations = new HashSet<>();

    @OneToMany(mappedBy = "agent")
    private Set<PlanningTravaux> programmations = new HashSet<>();

    public void addRole(Role role) { this.roles.add(role); }
    public void removeRole(Role role) { this.roles.remove(role); }
}