package com.gescli.ProgrammationTravaux.repository;

import com.gescli.ProgrammationTravaux.entity.TypeTravaux;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TypeTravauxRepository extends JpaRepository<TypeTravaux, String> {
    Optional<TypeTravaux> findByLibelle(String libelle);
}