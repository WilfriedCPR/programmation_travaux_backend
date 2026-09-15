package com.gescli.ProgrammationTravaux.repository;

import com.gescli.ProgrammationTravaux.entity.AffectationDevis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AffectationDevisRepository extends JpaRepository<AffectationDevis, String> {
    Optional<AffectationDevis> findByAgentIdAndDevisIdAndActiveTrue(String agentId, String devisId);
    Optional<AffectationDevis> findTopByAgentIdAndDevisIdAndActiveFalseOrderByDateAffectationDesc(String agentId, String devisId);
    List<AffectationDevis> findByAgentIdAndActiveTrue(String agentId);
    List<AffectationDevis> findByDevisIdAndActiveTrue(String devisId);
    List<AffectationDevis> findByAgentIdOrderByDateAffectationDesc(String agentId);
    List<AffectationDevis> findByDevisIdOrderByDateAffectationDesc(String devisId);
    long countByAgentIdAndActiveTrue(String agentId);
    long countByActiveTrue();
    long countByActiveTrueAndDateAffectationBetween(LocalDateTime start, LocalDateTime end);

    @Query("select count(distinct a.agent.id) from AffectationDevis a where a.active = true")
    long countDistinctAgentsActive();

    @Query("select count(distinct a.agent.id) from AffectationDevis a where a.active = true and a.dateAffectation between :start and :end")
    long countDistinctAgentsActiveBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
