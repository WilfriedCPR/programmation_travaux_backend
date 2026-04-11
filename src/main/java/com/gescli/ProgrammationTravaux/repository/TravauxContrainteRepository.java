package com.gescli.ProgrammationTravaux.repository;

import com.gescli.ProgrammationTravaux.entity.TravauxContrainte;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface TravauxContrainteRepository extends JpaRepository<TravauxContrainte, String> {
    List<TravauxContrainte> findByTravaux_Id(String travauxId);
    List<TravauxContrainte> findByPlanning_Id(String planningId);
    long countByTravaux_IdAndActiveTrue(String travauxId);
    long countByPlanning_IdAndActiveTrue(String planningId);

    @Query("SELECT DISTINCT c.contrainte.libelle FROM TravauxContrainte c WHERE c.contrainte.libelle IS NOT NULL " +
           "AND (:q IS NULL OR LOWER(c.contrainte.libelle) LIKE LOWER(CONCAT('%',:q,'%')))")
    List<String> findDistinctLibelles(@Param("q") String q);
}