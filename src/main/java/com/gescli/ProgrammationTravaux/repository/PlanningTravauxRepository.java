package com.gescli.ProgrammationTravaux.repository;

import com.gescli.ProgrammationTravaux.entity.PlanningTravaux;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PlanningTravauxRepository extends JpaRepository<PlanningTravaux, String> {

    @EntityGraph(attributePaths = {"participants", "participantsExternes"})
    List<PlanningTravaux> findByDevisId(String devisId);

    @EntityGraph(attributePaths = {"participants", "participantsExternes"})
    List<PlanningTravaux> findByDevisIdAndDeletedFalse(String devisId);

    @EntityGraph(attributePaths = {"participants", "participantsExternes"})
    List<PlanningTravaux> findByDevisIdAndDeletedTrue(String devisId);

    @EntityGraph(attributePaths = {"participants", "participantsExternes"})
    List<PlanningTravaux> findByDevisIdAndClotureTrue(String devisId);

    @EntityGraph(attributePaths = {"participants", "participantsExternes"})
    Optional<PlanningTravaux> findById(String id);

    @Query("SELECT COUNT(p) FROM PlanningTravaux p WHERE p.deleted = false AND p.dateDebut >= :start AND p.dateDebut < :end")
    long countByDateRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(p) FROM PlanningTravaux p WHERE p.deleted=false AND p.cloture=false AND p.dateFin IS NOT NULL AND p.dateFin < :now")
    long countOverdue(@Param("now") LocalDateTime now);

    @EntityGraph(attributePaths = {"devis", "travaux"})
    List<PlanningTravaux> findTop5ByOrderByDateDebutDesc();

    @Query("""
        SELECT DISTINCT p FROM PlanningTravaux p
        LEFT JOIN p.participants part
        WHERE p.deleted = false AND p.cloture = false AND p.dateFin IS NULL
          AND (p.agent.id = :agentId OR part.id = :agentId OR EXISTS (
            SELECT member.id FROM AffectationDevis member
            WHERE member.agent.id = :agentId
              AND member.devis.id = p.devis.id
              AND member.active = true
          ))
          AND EXISTS (
            SELECT aff.id FROM AffectationDevis aff
            WHERE aff.agent.id = :agentId
              AND aff.devis.id = p.devis.id
              AND aff.active = true
          )
        """)
    List<PlanningTravaux> findOpenForAgent(@Param("agentId") String agentId);

    @EntityGraph(attributePaths = {"devis", "travaux", "participants", "participantsExternes"})
    @Query("""
        SELECT DISTINCT p FROM PlanningTravaux p
        LEFT JOIN p.participants part
        WHERE p.deleted = false
          AND (p.agent.id = :agentId OR part.id = :agentId)
          AND EXISTS (
            SELECT aff.id FROM AffectationDevis aff
            WHERE aff.agent.id = :agentId
              AND aff.devis.id = p.devis.id
              AND aff.active = true
          )
        """)
    List<PlanningTravaux> findVisibleForAgent(@Param("agentId") String agentId);

    @Query("""
        SELECT COUNT(DISTINCT a.id) FROM Agent a
        WHERE a.actif = true AND EXISTS (
          SELECT p.id FROM PlanningTravaux p
          LEFT JOIN p.participants part
          WHERE p.deleted = false AND p.cloture = false
            AND (p.agent.id = a.id OR part.id = a.id)
            AND p.dateDebut <= :now
            AND (p.dateFin IS NULL OR p.dateFin >= :now)
            AND EXISTS (
              SELECT aff.id FROM AffectationDevis aff
              WHERE aff.agent.id = a.id
                AND aff.devis.id = p.devis.id
                AND aff.active = true
            )
        )
        """)
    long countAgentsCurrentlyBusy(@Param("now") LocalDateTime now);
}
