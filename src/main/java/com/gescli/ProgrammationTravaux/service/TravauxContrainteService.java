package com.gescli.ProgrammationTravaux.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.gescli.ProgrammationTravaux.dto.TravauxContrainteRequestDTO;
import com.gescli.ProgrammationTravaux.dto.TravauxContrainteResponseDTO;
import com.gescli.ProgrammationTravaux.exception.ConflictException;
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
        ensurePrivilegedMutation();
        Travaux travaux = travauxRepository.findById(dto.getTravauxId())
                .orElseThrow(() -> new EntityNotFoundException("Travaux not found with id: " + dto.getTravauxId()));

        TravauxContrainte entity = new TravauxContrainte();
        entity.setTravaux(travaux);
        Contrainte contrainte = resolveContrainte(dto.getContrainteLibelle());
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
        ensurePlanningVisible(planning);
        ensurePlanningMutable(planning);
        TravauxContrainte entity = new TravauxContrainte();
        entity.setPlanning(planning);
        entity.setTravaux(planning.getTravaux());
        Contrainte contrainte = resolveContrainte(contrainteLibelle);
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
        ensurePlanningVisible(entity.getPlanning());
        ensurePlanningMutable(entity.getPlanning());
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
        ensurePlanningVisible(entity.getPlanning());
        ensurePlanningMutable(entity.getPlanning());
        entity.setDateLeveeContrainte(LocalDateTime.now());
        entity.setObservationLevee(observationLevee);
        entity.setActive(false);
        return travauxContrainteMapper.toDto(travauxContrainteRepository.save(entity));
    }

    @Transactional
    public void deleteContrainte(String id) {
        ensurePrivilegedMutation();
        TravauxContrainte entity = travauxContrainteRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("TravauxContrainte not found: " + id));
        if (Boolean.TRUE.equals(entity.getActive())) {
            throw new IllegalStateException("Seule une contrainte levée peut être supprimée.");
        }
        ensurePlanningMutable(entity.getPlanning());
        travauxContrainteRepository.delete(entity);
    }

    @Transactional(readOnly = true)
    public List<TravauxContrainteResponseDTO> getContraintesByTravail(String travauxId) {
        ensurePrivilegedMutation();
        return travauxContrainteRepository.findByTravaux_Id(travauxId).stream()
                .map(travauxContrainteMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TravauxContrainteResponseDTO> getContraintesByPlanning(String planningId) {
        PlanningTravaux planning = planningTravauxRepository.findById(planningId)
                .orElseThrow(() -> new EntityNotFoundException("PlanningTravaux not found with id: " + planningId));
        ensurePlanningVisible(planning);
        return travauxContrainteRepository.findByPlanning_Id(planningId).stream()
                .map(travauxContrainteMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public TravauxContrainteResponseDTO update(String id, TravauxContrainteRequestDTO dto) {
        ensurePrivilegedMutation();
        TravauxContrainte entity = travauxContrainteRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("TravauxContrainte not found with id: " + id));
        if (dto.getContrainteLibelle() != null && !dto.getContrainteLibelle().isBlank()) {
            Contrainte contrainte = resolveContrainte(dto.getContrainteLibelle());
            entity.setContrainte(contrainte);
        }
        if (dto.getObservationMise() != null) {
            entity.setObservationMise(dto.getObservationMise());
        }
        return travauxContrainteMapper.toDto(travauxContrainteRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public List<TravauxContrainteResponseDTO> getAllContraintes() {
        ensurePrivilegedMutation();
        return travauxContrainteRepository.findAll().stream()
                .map(travauxContrainteMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<String> suggestLibelles(String query) {
        if (query == null || query.isBlank()) {
            List<String> list = contrainteRepository.findTop50ByOrderByLibelleAsc().stream()
                    .map(Contrainte::getLibelle)
                    .filter(s -> s != null && !s.isBlank())
                    .distinct()
                    .sorted()
                    .limit(50)
                    .collect(Collectors.toList());
            return list;
        }
        List<String> list = contrainteRepository.findByLibelleContainingIgnoreCase(query).stream()
                .map(Contrainte::getLibelle)
                .filter(s -> s != null && !s.isBlank())
                .distinct()
                .collect(Collectors.toList());
        return list.stream().sorted(String.CASE_INSENSITIVE_ORDER).limit(50).toList();
    }

    private Contrainte resolveContrainte(String rawLibelle) {
        if (rawLibelle == null || rawLibelle.isBlank()) {
            throw new IllegalArgumentException("Le type de contrainte est obligatoire.");
        }
        String libelle = rawLibelle.trim().replaceAll("\\s+", " ");
        return contrainteRepository.findFirstByLibelleIgnoreCase(libelle)
                .orElseGet(() -> {
                    Contrainte c = new Contrainte();
                    c.setLibelle(libelle);
                    c.setCreatedDate(LocalDateTime.now());
                    c.setDeleted(false);
                    return contrainteRepository.saveAndFlush(c);
                });
    }

    private boolean isRestrictedAgent() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return false;
        boolean privileged = auth.getAuthorities().stream().anyMatch(a ->
                "ROLE_ADMIN".equals(a.getAuthority()) || "ROLE_CHEF".equals(a.getAuthority()));
        boolean agent = auth.getAuthorities().stream().anyMatch(a -> "ROLE_AGENT".equals(a.getAuthority()));
        return agent && !privileged;
    }

    private void ensurePrivilegedMutation() {
        if (isRestrictedAgent()) {
            throw new AccessDeniedException("Cette opération est réservée au Chef ou à l'Administrateur.");
        }
    }

    private void ensurePlanningMutable(PlanningTravaux planning) {
        if (planning != null && (planning.isDeleted() || planning.isCloture() || planning.getDateFin() != null)) {
            throw new ConflictException("Ce planning est en lecture seule car il est terminÃ©.");
        }
    }

    private void ensurePlanningVisible(PlanningTravaux planning) {
        if (planning == null) {
            if (isRestrictedAgent()) throw new AccessDeniedException("La contrainte n'est pas rattachée à un planning autorisé.");
            return;
        }
        if (!isRestrictedAgent()) return;
        String agentId = SecurityContextHolder.getContext().getAuthentication().getName();
        boolean visible = (planning.getAgent() != null && agentId.equals(planning.getAgent().getId()))
                || planning.getParticipants().stream().anyMatch(a -> agentId.equals(a.getId()))
                || (planning.getDevis() != null && planning.getDevis().getAffectations().stream()
                .anyMatch(a -> a.isActive() && a.getAgent() != null && agentId.equals(a.getAgent().getId())));
        if (!visible) throw new AccessDeniedException("Accès à la contrainte non autorisé.");
    }
}
