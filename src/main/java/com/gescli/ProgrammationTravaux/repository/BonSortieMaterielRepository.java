package com.gescli.ProgrammationTravaux.repository;

import com.gescli.ProgrammationTravaux.entity.BonSortieMateriel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BonSortieMaterielRepository extends JpaRepository<BonSortieMateriel, String> {
    boolean existsByNumeroBon(String numeroBon);
    Optional<BonSortieMateriel> findByNumeroBon(String numeroBon);
    @Query("SELECT b FROM BonSortieMateriel b WHERE b.bonInitial IS NULL")
    List<BonSortieMateriel> findBonsInitiaux();
    List<BonSortieMateriel> findByBonInitial(BonSortieMateriel bonInitial);
    List<BonSortieMateriel> findByClientNomContainingIgnoreCase(String clientNom);
    List<BonSortieMateriel> findByNumeroDevis(String numeroDevis);
    List<BonSortieMateriel> findByNumeroDevisContainingIgnoreCase(String numeroDevis);
    List<BonSortieMateriel> findByDemandeMateriel_Devis_Id(String devisId);
    long countByDemandeMateriel_Devis_Id(String devisId);
    long countByDemandeMateriel_Id(String demandeMaterielId);
    List<BonSortieMateriel> findByDateSortieBetween(LocalDateTime debut, LocalDateTime fin);
    long countByDateSortieBetween(LocalDateTime debut, LocalDateTime fin);
    Optional<BonSortieMateriel> findFirstByDemandeMateriel_IdAndBonInitialIsNull(String demandeMaterielId);
    long countByNumeroDevisIgnoreCase(String numeroDevis);
    List<BonSortieMateriel> findTop5ByOrderByDateSortieDesc();
}