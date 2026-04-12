package com.gescli.ProgrammationTravaux.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gescli.ProgrammationTravaux.dto.TravauxContrainteRequestDTO;
import com.gescli.ProgrammationTravaux.dto.TravauxContrainteResponseDTO;
import com.gescli.ProgrammationTravaux.entity.Contrainte;
import com.gescli.ProgrammationTravaux.entity.PlanningTravaux;
import com.gescli.ProgrammationTravaux.entity.Travaux;
import com.gescli.ProgrammationTravaux.entity.TravauxContrainte;
import com.gescli.ProgrammationTravaux.mapper.TravauxContrainteMapper;
import com.gescli.ProgrammationTravaux.repository.ContrainteRepository;
import com.gescli.ProgrammationTravaux.repository.PlanningTravauxRepository;
import com.gescli.ProgrammationTravaux.repository.TravauxContrainteRepository;
import com.gescli.ProgrammationTravaux.repository.TravauxRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TravauxContrainteService {

    private final TravauxContrainteRepository travauxContrainteRepository;
    private final TravauxRepository travauxRepository;
    private final PlanningTravauxRepository planningTravauxRepository;
    private final TravauxContrainteMapper travauxContrainteMapper;
    private final ContrainteRepository contrainteRepository;

    @Transactional
    public TravauxContrainteResponseDTO addContrainteToTravail(TravauxContrainteRequestDTO dto) {
        Travaux travaux = travauxRepository.findById(dto.getTravauxId())
                .orElseThrow(() -> new EntityNotFoundException("Travaux not found with id: " + dto.getTravauxId()));

        TravauxContrainte entity = new TravauxContrainte();
        entity.setTravaux(travaux);
        Contrainte contrainte = contrainteRepository
                .findFirstByLibelleIgnoreCase(dto.getContrainteLibelle())
                .orElseGet(() -> {
                    Contrainte c = new Contrainte();
                    c.setLibelle(dto.getContrainteLibelle());
                    return contrainteRepository.save(c);
                });
        entity.setContrainte(contrainte);
        entity.setDateMiseContrainte(LocalDateTime.now());
        entity.setObservationMise(dto.getObservationMise());
        entity.setActive(true);

        return travauxContrainteMapper.toDto(travauxContrainteRepository.save(entity));
    }

    @Transactional
    public TravauxContrainteResponseDTO addContrainteToPlanning(String planningId, String contrainteLibelle,
            String observationMise) {
        PlanningTravaux planning = planningTravauxRepository.findById(planningId)
                .orElseThrow(() -> new EntityNotFoundException("PlanningTravaux not found with id: " + planningId));
        TravauxContrainte entity = new TravauxContrainte();
        entity.setPlanning(planning);
        entity.setTravaux(planning.getTravaux());
        Contrainte contrainte = contrainteRepository
                .findFirstByLibelleIgnoreCase(contrainteLibelle)
                .orElseGet(() -> {
                    Contrainte c = new Contrainte();
                    c.setLibelle(contrainteLibelle);
                    return contrainteRepository.save(c);
                });
        entity.setContrainte(contrainte);
        entity.setDateMiseContrainte(LocalDateTime.now());
        entity.setObservationMise(observationMise);
        entity.setActive(true);
        return travauxContrainteMapper.toDto(travauxContrainteRepository.save(entity));
    }

    @Transactional
    public TravauxContrainteResponseDTO removeContrainteFromTravail(String id, String observationLevee) {
        TravauxContrainte entity = travauxContrainteRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("TravauxContrainte not found with id: " + id));
        entity.setDateLeveeContrainte(LocalDateTime.now());
        entity.setObservationLevee(observationLevee);
        entity.setActive(false);
        return travauxContrainteMapper.toDto(travauxContrainteRepository.save(entity));
    }

    @Transactional
    public TravauxContrainteResponseDTO leverContrainte(String planningId, String contrainteId,
            String observationLevee) {
        TravauxContrainte entity = travauxContrainteRepository.findById(contrainteId)
                .orElseThrow(() -> new EntityNotFoundException("TravauxContrainte not found with id: " + contrainteId));
        if (entity.getPlanning() == null || !entity.getPlanning().getId().equals(planningId)) {
            throw new EntityNotFoundException("Contrainte does not belong to planning: " + planningId);
        }
        entity.setDateLeveeContrainte(LocalDateTime.now());
        entity.setObservationLevee(observationLevee);
        entity.setActive(false);
        return travauxContrainteMapper.toDto(travauxContrainteRepository.save(entity));
    }

    @Transactional
    public void deleteContrainte(String id) {
        if (!travauxContrainteRepository.existsById(id)) {
            throw new EntityNotFoundException("TravauxContrainte not found with id: " + id);
        }
        travauxContrainteRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<TravauxContrainteResponseDTO> getContraintesByTravail(String travauxId) {
        return travauxContrainteRepository.findByTravaux_Id(travauxId).stream()
                .map(travauxContrainteMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TravauxContrainteResponseDTO> getContraintesByPlanning(String planningId) {
        return travauxContrainteRepository.findByPlanning_Id(planningId).stream()
                .map(travauxContrainteMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public TravauxContrainteResponseDTO update(String id, TravauxContrainteRequestDTO dto) {
        TravauxContrainte entity = travauxContrainteRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("TravauxContrainte not found with id: " + id));
        if (dto.getContrainteLibelle() != null && !dto.getContrainteLibelle().isBlank()) {
            Contrainte contrainte = contrainteRepository
                    .findFirstByLibelleIgnoreCase(dto.getContrainteLibelle())
                    .orElseGet(() -> {
                        Contrainte c = new Contrainte();
                        c.setLibelle(dto.getContrainteLibelle());
                        return contrainteRepository.save(c);
                    });
            entity.setContrainte(contrainte);
        }
        if (dto.getObservationMise() != null) {
            entity.setObservationMise(dto.getObservationMise());
        }
        return travauxContrainteMapper.toDto(travauxContrainteRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public List<TravauxContrainteResponseDTO> getAllContraintes() {
        return travauxContrainteRepository.findAll().stream()
                .map(travauxContrainteMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<String> suggestLibelles(String query) {
        if (query == null || query.isBlank()) {
            List<String> list = contrainteRepository.findAll().stream()
                    .map(Contrainte::getLibelle)
                    .filter(s -> s != null && !s.isBlank())
                    .distinct()
                    .sorted()
                    .limit(50)
                    .collect(Collectors.toList());
            System.out.println("SUGGEST EMPTY QUERY: found " + list.size() + " elements in DB");
            return list;
        }
        List<String> list = contrainteRepository.findByLibelleContainingIgnoreCase(query).stream()
                .map(Contrainte::getLibelle)
                .filter(s -> s != null && !s.isBlank())
                .distinct()
                .collect(Collectors.toList());
        System.out.println("SUGGEST QUERY '" + query + "': found " + list.size() + " elements in DB");
        return list;
    }
}
