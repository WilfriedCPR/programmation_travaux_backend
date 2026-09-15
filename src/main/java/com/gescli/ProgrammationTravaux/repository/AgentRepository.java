package com.gescli.ProgrammationTravaux.repository;

import com.gescli.ProgrammationTravaux.entity.Agent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AgentRepository extends JpaRepository<Agent, String> {
    Optional<Agent> findByNomAndPrenom(String nom, String prenom);
    Optional<Agent> findByCode(String code);
    long countByActifTrue();
    long countByActifFalse();
}
