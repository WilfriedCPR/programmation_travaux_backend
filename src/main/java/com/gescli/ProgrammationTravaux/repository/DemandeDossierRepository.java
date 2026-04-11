package com.gescli.ProgrammationTravaux.repository;

import com.gescli.ProgrammationTravaux.entity.DemandeDossier;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DemandeDossierRepository extends JpaRepository<DemandeDossier, String> {}