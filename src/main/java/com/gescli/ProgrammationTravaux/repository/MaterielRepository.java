package com.gescli.ProgrammationTravaux.repository;

import com.gescli.ProgrammationTravaux.entity.Materiel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MaterielRepository extends JpaRepository<Materiel, String> {}