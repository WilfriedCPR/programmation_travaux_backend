package com.gescli.ProgrammationTravaux.repository;

import com.gescli.ProgrammationTravaux.entity.Reference;
import com.gescli.ProgrammationTravaux.entity.TypeReference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReferenceRepository extends JpaRepository<Reference, String> {
    List<Reference> findByTypeReference(TypeReference typeReference);
    boolean existsByReference(String reference);
}
