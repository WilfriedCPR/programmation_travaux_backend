package com.gescli.ProgrammationTravaux.repository;

import com.gescli.ProgrammationTravaux.entity.Contrainte;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContrainteRepository extends JpaRepository<Contrainte, UUID> {
    Optional<Contrainte> findByLibelle(String libelle);
    Optional<Contrainte> findFirstByLibelleIgnoreCase(String libelle);
    List<Contrainte> findByLibelleContainingIgnoreCase(String libelle);
}