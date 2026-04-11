package com.gescli.ProgrammationTravaux.repository;

import com.gescli.ProgrammationTravaux.entity.DevisMateriel;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DevisMaterielRepository extends JpaRepository<DevisMateriel, String> {
    List<DevisMateriel> findByDevisId(String devisId);
}