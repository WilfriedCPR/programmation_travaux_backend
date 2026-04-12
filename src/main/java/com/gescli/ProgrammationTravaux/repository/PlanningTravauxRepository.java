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

    @Query("SELECT p FROM PlanningTravaux p JOIN FETCH p.devis ORDER BY p.dateDebut DESC LIMIT 5")
    List<PlanningTravaux> findTop5WithDevisOrderByDateDebutDesc();
}