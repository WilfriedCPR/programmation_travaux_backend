package com.gescli.ProgrammationTravaux.repository;

import com.gescli.ProgrammationTravaux.entity.Devis;
import com.gescli.ProgrammationTravaux.entity.DevisStatut;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface DevisRepository extends JpaRepository<Devis, String> {

    Optional<Devis> findByDevisCode(String devisCode);
    long countByStatut(DevisStatut statut);

    Page<Devis> findAllByStatut(DevisStatut statut, Pageable pageable);

    @Query("SELECT d FROM Devis d LEFT JOIN d.dossier dd LEFT JOIN dd.client c " +
           "WHERE LOWER(d.devisCode) LIKE LOWER(CONCAT('%',:q,'%')) " +
           "OR LOWER(COALESCE(c.cliCode,'')) LIKE LOWER(CONCAT('%',:q,'%')) " +
           "OR LOWER(CONCAT(COALESCE(c.cliNom,''),' ',COALESCE(c.cliPrenom,''),' ',COALESCE(c.cliRaisonSocial,''))) LIKE LOWER(CONCAT('%',:q,'%')) " +
           "OR LOWER(COALESCE(dd.demOption,'')) LIKE LOWER(CONCAT('%',:q,'%'))")
    Page<Devis> search(@Param("q") String q, Pageable pageable);

    @Query("SELECT d FROM Devis d LEFT JOIN d.dossier dd LEFT JOIN dd.client c " +
           "WHERE d.statut = :statut AND (" +
           "LOWER(d.devisCode) LIKE LOWER(CONCAT('%',:q,'%')) " +
           "OR LOWER(COALESCE(c.cliCode,'')) LIKE LOWER(CONCAT('%',:q,'%')) " +
           "OR LOWER(CONCAT(COALESCE(c.cliNom,''),' ',COALESCE(c.cliPrenom,''),' ',COALESCE(c.cliRaisonSocial,''))) LIKE LOWER(CONCAT('%',:q,'%')) " +
           "OR LOWER(COALESCE(dd.demOption,'')) LIKE LOWER(CONCAT('%',:q,'%')))")
    Page<Devis> searchByStatut(@Param("q") String q, @Param("statut") DevisStatut statut, Pageable pageable);

    List<Devis> findTop5ByOrderByDateCreationDesc();

    List<Devis> findAllByStatut(DevisStatut statut);

    @Query(value = "SELECT * FROM tr_devis WHERE LOWER(devis_status) NOT IN ('clos','cloture','clôture','inactif','closed')", nativeQuery = true)
    List<Devis> findAllEnCours();

    @Query(value = "SELECT * FROM tr_devis WHERE LOWER(devis_status) IN ('clos','cloture','clôture','inactif','closed')", nativeQuery = true)
    List<Devis> findAllClos();

    @Query(value = "SELECT COUNT(*) FROM tr_devis WHERE LOWER(devis_status) NOT IN ('clos','cloture','clôture','inactif','closed')", nativeQuery = true)
    long countEnCours();

    @Query(value = "SELECT COUNT(*) FROM tr_devis WHERE LOWER(devis_status) IN ('clos','cloture','clôture','inactif','closed')", nativeQuery = true)
    long countClos();

    @Query("SELECT d FROM Devis d LEFT JOIN d.dossier dd LEFT JOIN dd.client c " +
           "WHERE LOWER(d.devisCode) LIKE LOWER(CONCAT('%',:q,'%')) " +
           "OR LOWER(COALESCE(c.cliCode,'')) LIKE LOWER(CONCAT('%',:q,'%')) " +
           "OR LOWER(CONCAT(COALESCE(c.cliNom,''),' ',COALESCE(c.cliPrenom,''),' ',COALESCE(c.cliRaisonSocial,''))) LIKE LOWER(CONCAT('%',:q,'%')) " +
           "OR LOWER(COALESCE(dd.demOption,'')) LIKE LOWER(CONCAT('%',:q,'%'))")
    List<Devis> searchAll(@Param("q") String q);

    @Query("SELECT d FROM Devis d LEFT JOIN d.dossier dd LEFT JOIN dd.client c " +
           "WHERE d.statut = :statut AND (" +
           "LOWER(d.devisCode) LIKE LOWER(CONCAT('%',:q,'%')) " +
           "OR LOWER(COALESCE(c.cliCode,'')) LIKE LOWER(CONCAT('%',:q,'%')) " +
           "OR LOWER(CONCAT(COALESCE(c.cliNom,''),' ',COALESCE(c.cliPrenom,''),' ',COALESCE(c.cliRaisonSocial,''))) LIKE LOWER(CONCAT('%',:q,'%')) " +
           "OR LOWER(COALESCE(dd.demOption,'')) LIKE LOWER(CONCAT('%',:q,'%')))")
    List<Devis> searchAllByStatut(@Param("q") String q, @Param("statut") DevisStatut statut);
}