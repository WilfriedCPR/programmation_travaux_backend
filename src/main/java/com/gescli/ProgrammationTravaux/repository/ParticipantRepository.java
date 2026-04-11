package com.gescli.ProgrammationTravaux.repository;

import com.gescli.ProgrammationTravaux.entity.Participant;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ParticipantRepository extends JpaRepository<Participant, String> {
    Optional<Participant> findByNomAndPrenom(String nom, String prenom);
    List<Participant> findByExterneTrue();
    long countByExterneTrueAndDateCreationBetween(LocalDateTime start, LocalDateTime end);
    long countByExterneTrue();
}