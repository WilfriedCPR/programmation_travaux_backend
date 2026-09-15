package com.gescli.ProgrammationTravaux.repository;

import com.gescli.ProgrammationTravaux.entity.DemandeMateriel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDateTime;
import java.util.List;

public interface DemandeMaterielRepository extends JpaRepository<DemandeMateriel, String> {
    List<DemandeMateriel> findByDeletedFalse();
    List<DemandeMateriel> findByDeletedTrue();
    List<DemandeMateriel> findByDevisIdAndDeletedFalse(String devisId);
    List<DemandeMateriel> findByDevisIdAndDeletedTrue(String devisId);
    long countByValideFalseAndDeletedFalse();
    long countByDeletedFalseAndDateDemandeBetween(LocalDateTime start, LocalDateTime end);

    @Query("""
        SELECT COUNT(d) FROM DemandeMateriel d
        WHERE d.deleted = false AND d.valide = true
          AND NOT EXISTS (
            SELECT b.id FROM BonSortieMateriel b
            WHERE b.demandeMateriel = d AND b.bonInitial IS NULL
          )
        """)
    long countBonsSortieATraiter();
    boolean existsByDevisIdAndReferenceValueAndDeletedFalse(String devisId, String referenceValue);
    boolean existsByDevisIdAndReferenceValueAndIdNotAndDeletedFalse(String devisId, String referenceValue, String id);
}