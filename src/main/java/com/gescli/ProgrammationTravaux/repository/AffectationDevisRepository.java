package com.gescli.ProgrammationTravaux.repository;

import com.gescli.ProgrammationTravaux.entity.AffectationDevis;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AffectationDevisRepository extends JpaRepository<AffectationDevis, String> {
    Optional<AffectationDevis> findByAgentIdAndDevisIdAndActiveTrue(String agentId, String devisId);
    Optional<AffectationDevis> findByAgentIdAndDevisIdAndActiveFalse(String agentId, String devisId);
    List<AffectationDevis> findByAgentIdAndActiveTrue(String agentId);
    List<AffectationDevis> findByDevisId(String devisId);
    long countByActiveTrue();
    long countByActiveTrueAndDateAffectationBetween(LocalDateTime start, LocalDateTime end);
}