package com.gescli.ProgrammationTravaux.repository;

import com.gescli.ProgrammationTravaux.entity.Travaux;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TravauxRepository extends JpaRepository<Travaux, String> {}