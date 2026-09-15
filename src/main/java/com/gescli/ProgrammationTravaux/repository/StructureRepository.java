package com.gescli.ProgrammationTravaux.repository;

import com.gescli.ProgrammationTravaux.entity.Structure;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface StructureRepository extends JpaRepository<Structure, String> {
    Optional<Structure> findFirstByLibelleIgnoreCase(String libelle);
}
