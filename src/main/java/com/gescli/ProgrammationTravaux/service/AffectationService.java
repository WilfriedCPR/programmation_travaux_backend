package com.gescli.ProgrammationTravaux.service;

import com.gescli.ProgrammationTravaux.dto.AffectationRequestDTO;
import com.gescli.ProgrammationTravaux.dto.AffectationResponseDTO;
import com.gescli.ProgrammationTravaux.dto.ActivityLogDTO;
import com.gescli.ProgrammationTravaux.exception.ConflictException;
import com.gescli.ProgrammationTravaux.entity.*;
import com.gescli.ProgrammationTravaux.mapper.AffectationMapper;
import com.gescli.ProgrammationTravaux.repository.AffectationDevisRepository;
import com.gescli.ProgrammationTravaux.repository.AgentRepository;
import com.gescli.ProgrammationTravaux.repository.DevisRepository;
import com.gescli.ProgrammationTravaux.repository.PlanningTravauxRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AffectationService {

    private final AffectationDevisRepository affectationRepo;
    private final AgentRepository agentRepo;
    private final DevisRepository devisRepo;
    private final PlanningTravauxRepository planningRepo;
    private final AffectationMapper mapper;
    private final ActivityLogService activityLogService;
    private final NotificationService notificationService;

    @Transactional
    public AffectationResponseDTO affecterAgent(AffectationRequestDTO dto) {
        Agent agent = agentRepo.findById(dto.getAgentId())
                .orElseThrow(() -> new EntityNotFoundException("Agent introuvable : " + dto.getAgentId()));
        if (!agent.isActif()) throw new IllegalStateException("Cet agent est inactif et ne peut pas être affecté.");
        Devis devis = devisRepo.findById(dto.getDevisId())
                .orElseThrow(() -> new EntityNotFoundException("Devis introuvable : " + dto.getDevisId()));
        ensureNoTerminatedPlanning(devis.getId());

        if (affectationRepo.findByAgentIdAndDevisIdAndActiveTrue(dto.getAgentId(), dto.getDevisId()).isPresent()) {
            throw new IllegalStateException("L'agent est déjà affecté à ce devis.");
        }

        List<String> conflicts = detectPlanningConflicts(agent.getId(), devis.getId());
        if (!conflicts.isEmpty() && !dto.isForce()) {
            throw new IllegalStateException("Conflit de planning détecté : " + String.join(" | ", conflicts)
                    + ". Confirmez explicitement l'affectation pour continuer.");
        }

        AffectationDevis affectation = new AffectationDevis();
        affectation.setAgent(agent);
        affectation.setDevis(devis);
        affectation.setDateAffectation(LocalDateTime.now());
        affectation.setActive(true);
        affectation.setAffectePar(activityLogService.currentActor());
        affectation.setRetirePar(null);

        AffectationDevis saved = affectationRepo.save(affectation);
        activityLogService.log("AFFECTATION_AJOUT",
                fullName(agent) + " affecté au devis " + devis.getDevisCode(), devis.getDevisCode(), devis.getId(), agent.getId());
        notificationService.notifyAgent(agent.getId(), "Nouvelle affectation",
                "Vous avez été affecté au devis " + devis.getDevisCode() + ".", "AFFECTATION", "/devis/" + devis.getId());

        AffectationResponseDTO response = toDto(saved);
        response.setPlanningConflict(!conflicts.isEmpty());
        response.setConflictMessages(conflicts);
        response.setDevisDejaAffectes(affectationRepo.findByAgentIdAndActiveTrue(dto.getAgentId()).stream()
                .map(a -> a.getDevis().getDevisCode()).distinct().toList());
        return response;
    }

    @Transactional
    public void retirerAgent(String devisId, String agentId) {
        AffectationDevis aff = affectationRepo.findByAgentIdAndDevisIdAndActiveTrue(agentId, devisId)
                .orElseThrow(() -> new EntityNotFoundException("Affectation active introuvable pour agent=" + agentId + ", devis=" + devisId));
        ensureNoTerminatedPlanning(devisId);
        aff.setActive(false);
        aff.setDateRetraitAffectation(LocalDateTime.now());
        aff.setRetirePar(activityLogService.currentActor());
        affectationRepo.save(aff);

        // Un agent retiré d'un devis ne doit plus rester membre opérationnel de ses
        // plannings ouverts. Les plannings clôturés restent intacts pour l'historique.
        List<PlanningTravaux> affectedPlannings = planningRepo.findByDevisIdAndDeletedFalse(devisId).stream()
                .filter(p -> !p.isCloture())
                .filter(p -> (p.getAgent() != null && agentId.equals(p.getAgent().getId()))
                        || (p.getParticipants() != null && p.getParticipants().stream().anyMatch(a -> agentId.equals(a.getId()))))
                .toList();
        for (PlanningTravaux planning : affectedPlannings) {
            boolean changed = false;
            if (planning.getAgent() != null && agentId.equals(planning.getAgent().getId())) {
                planning.setAgent(null);
                changed = true;
            }
            if (planning.getParticipants() != null) {
                changed = planning.getParticipants().removeIf(a -> agentId.equals(a.getId())) || changed;
            }
            if (changed) planningRepo.save(planning);
        }

        activityLogService.log("AFFECTATION_RETRAIT",
                fullName(aff.getAgent()) + " retiré du devis " + aff.getDevis().getDevisCode(),
                aff.getDevis().getDevisCode(), aff.getDevis().getId(), aff.getAgent().getId());
        notificationService.notifyAgent(aff.getAgent().getId(), "Affectation retirée",
                "Vous avez été retiré du devis " + aff.getDevis().getDevisCode() + ".", "AFFECTATION", "/devis/" + aff.getDevis().getId());
    }

    @Transactional
    public void reactiverAgent(String devisId, String agentId) {
        Agent agent = agentRepo.findById(agentId).orElseThrow(() -> new EntityNotFoundException("Agent introuvable"));
        ensureNoTerminatedPlanning(devisId);
        if (!agent.isActif()) throw new IllegalStateException("Cet agent est inactif.");
        if (affectationRepo.findByAgentIdAndDevisIdAndActiveTrue(agentId, devisId).isPresent()) {
            throw new IllegalStateException("L'agent est déjà affecté activement à ce devis.");
        }

        // L'ancienne affectation inactive constitue une ligne d'historique et ne doit jamais
        // être remise à active, sinon sa date de retrait et son auteur seraient perdus.
        AffectationDevis previous = affectationRepo
                .findTopByAgentIdAndDevisIdAndActiveFalseOrderByDateAffectationDesc(agentId, devisId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Affectation inactive introuvable pour agent=" + agentId + ", devis=" + devisId));

        AffectationDevis newAffectation = new AffectationDevis();
        newAffectation.setAgent(agent);
        newAffectation.setDevis(previous.getDevis());
        newAffectation.setActive(true);
        newAffectation.setDateAffectation(LocalDateTime.now());
        newAffectation.setDateRetraitAffectation(null);
        newAffectation.setAffectePar(activityLogService.currentActor());
        newAffectation.setRetirePar(null);
        affectationRepo.save(newAffectation);

        activityLogService.log("AFFECTATION_REACTIVATION",
                fullName(agent) + " réaffecté au devis " + previous.getDevis().getDevisCode(),
                previous.getDevis().getDevisCode(), previous.getDevis().getId(), agentId);
    }


    @Transactional
    public int terminerAffectationsDuDevis(String devisId, String motif) {
        List<AffectationDevis> actives = affectationRepo.findByDevisIdAndActiveTrue(devisId);
        if (actives.isEmpty()) return 0;
        LocalDateTime now = LocalDateTime.now();
        for (AffectationDevis aff : actives) {
            aff.setActive(false);
            aff.setDateRetraitAffectation(now);
            aff.setRetirePar(activityLogService.currentActor());
            String code = aff.getDevis() != null ? aff.getDevis().getDevisCode() : devisId;
            activityLogService.log("AFFECTATION_FIN_AUTOMATIQUE",
                    fullName(aff.getAgent()) + " libéré du devis " + code + " (" + motif + ")",
                    code, devisId, aff.getAgent().getId());
            notificationService.notifyAgent(aff.getAgent().getId(), "Affectation terminée",
                    "Votre affectation au devis " + code + " est terminée : " + motif + ".",
                    "AFFECTATION", "/devis/" + devisId);
        }
        affectationRepo.saveAll(actives);
        return actives.size();
    }

    @Transactional(readOnly = true)
    public List<AffectationResponseDTO> getAgentsByDevisId(String devisId) {
        return affectationRepo.findByDevisIdOrderByDateAffectationDesc(devisId).stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<AffectationResponseDTO> getDevisByAgentId(String agentId) {
        return affectationRepo.findByAgentIdAndActiveTrue(agentId).stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<AffectationResponseDTO> getHistoryByAgentId(String agentId) {
        if (!agentRepo.existsById(agentId)) throw new EntityNotFoundException("Agent introuvable : " + agentId);
        List<AffectationResponseDTO> result = new ArrayList<>();
        result.addAll(affectationRepo.findByAgentIdAndActiveTrue(agentId).stream().map(this::toDto).toList());
        result.addAll(getInactiveHistoryByAgentId(agentId));
        result.sort(Comparator.comparing((AffectationResponseDTO a) ->
                a.getDateAffectation() != null ? a.getDateAffectation() : LocalDateTime.MIN).reversed());
        return result;
    }

    @Transactional(readOnly = true)
    public List<AffectationResponseDTO> getInactiveHistoryByAgentId(String agentId) {
        if (!agentRepo.existsById(agentId)) throw new EntityNotFoundException("Agent introuvable : " + agentId);

        List<AffectationResponseDTO> history = new ArrayList<>(
                affectationRepo.findByAgentIdAndActiveFalseOrderByDateRetraitAffectationDesc(agentId)
                        .stream().map(this::toDto).toList());

        // Compatibilité avec les anciennes versions : certaines réactivations réutilisaient
        // la même ligne et pouvaient faire disparaître un retrait historique. Le journal
        // d'activités est utilisé pour reconstituer ces retraits sans modifier la base.
        List<ActivityLogDTO> removals = activityLogService.historyEventsByAgent(agentId,
                List.of("AFFECTATION_RETRAIT", "AFFECTATION_FIN_AUTOMATIQUE"));
        for (ActivityLogDTO log : removals) {
            if (log.getDevisId() == null || log.getDateCreation() == null) continue;
            boolean alreadyRepresented = history.stream().anyMatch(h ->
                    Objects.equals(h.getDevisId(), log.getDevisId())
                    && h.getDateRetraitAffectation() != null
                    && Math.abs(java.time.Duration.between(h.getDateRetraitAffectation(), log.getDateCreation()).toMinutes()) <= 5);
            if (alreadyRepresented) continue;

            AffectationResponseDTO synthetic = new AffectationResponseDTO();
            synthetic.setId("LOG-" + log.getId());
            synthetic.setAgentId(agentId);
            synthetic.setDevisId(log.getDevisId());
            synthetic.setDevisCode(log.getReference());
            synthetic.setActive(false);
            synthetic.setDateRetraitAffectation(log.getDateCreation());
            synthetic.setRetirePar(log.getActeur());

            ActivityLogDTO addition = activityLogService.latestEventBefore(agentId, log.getDevisId(),
                    List.of("AFFECTATION_AJOUT", "AFFECTATION_REACTIVATION"), log.getDateCreation());
            if (addition != null) {
                synthetic.setDateAffectation(addition.getDateCreation());
                synthetic.setAffectePar(addition.getActeur());
            } else {
                synthetic.setDateAffectation(log.getDateCreation());
            }

            devisRepo.findById(log.getDevisId()).ifPresent(devis -> {
                synthetic.setDevisCode(devis.getDevisCode());
                synthetic.setDevisStatut(devis.getStatut() != null ? devis.getStatut().name() : null);
                if (devis.getDossier() != null && devis.getDossier().getClient() != null) {
                    Client c = devis.getDossier().getClient();
                    String client = c.getCliRaisonSocial();
                    if (client == null || client.isBlank()) {
                        client = ((c.getCliPrenom() == null ? "" : c.getCliPrenom()) + " "
                                + (c.getCliNom() == null ? "" : c.getCliNom())).trim();
                    }
                    synthetic.setClientLibelle(client);
                }
            });
            history.add(synthetic);
        }

        history.sort(Comparator.comparing((AffectationResponseDTO a) ->
                a.getDateRetraitAffectation() != null ? a.getDateRetraitAffectation() :
                        (a.getDateAffectation() != null ? a.getDateAffectation() : LocalDateTime.MIN)).reversed());
        return history;
    }

    @Transactional(readOnly = true)
    public List<String> detectPlanningConflicts(String agentId, String devisId) {
        List<PlanningTravaux> targets = planningRepo.findByDevisIdAndDeletedFalse(devisId).stream()
                .filter(p -> !p.isCloture()).toList();
        if (targets.isEmpty()) return List.of();
        Set<String> messages = new LinkedHashSet<>();
        for (PlanningTravaux target : targets) {
            LocalDateTime start = target.getDateDebut();
            if (start == null) continue;
            LocalDateTime end = effectiveEnd(target);
            for (PlanningTravaux conflict : planningRepo.findOpenForAgent(agentId)) {
                if (conflict.getId().equals(target.getId())) continue;
                if (conflict.getDevis() == null || devisId.equals(conflict.getDevis().getId())) continue;
                if (overlaps(start, end, conflict.getDateDebut(), effectiveEnd(conflict))) {
                    messages.add("Conflit avec le devis " + conflict.getDevis().getDevisCode() + " du "
                            + conflict.getDateDebut().toLocalDate() + " (" + conflict.getTravaux().getLibelle() + ")");
                }
            }
        }
        return new ArrayList<>(messages);
    }

    private AffectationResponseDTO toDto(AffectationDevis entity) {
        AffectationResponseDTO dto = mapper.toDto(entity);
        Devis devis = entity.getDevis();
        Agent agent = entity.getAgent();
        dto.setDevisStatut(devis.getStatut() != null ? devis.getStatut().name() : null);
        if (agent != null) {
            dto.setAgentRoles(agent.getRoles().stream().map(Role::getLibelle).collect(java.util.stream.Collectors.toSet()));
            if ((dto.getAffectePar() == null || dto.getAffectePar().isBlank()) && entity.getDateAffectation() != null) {
                dto.setAffectePar(activityLogService.findActorAround(agent.getId(), devis.getId(), entity.getDateAffectation(),
                        List.of("AFFECTATION_AJOUT", "AFFECTATION_REACTIVATION")));
            }
            if ((dto.getRetirePar() == null || dto.getRetirePar().isBlank()) && entity.getDateRetraitAffectation() != null) {
                dto.setRetirePar(activityLogService.findActorAround(agent.getId(), devis.getId(), entity.getDateRetraitAffectation(),
                        List.of("AFFECTATION_RETRAIT", "AFFECTATION_FIN_AUTOMATIQUE")));
            }
        }
        if (devis.getDossier() != null && devis.getDossier().getClient() != null) {
            Client c = devis.getDossier().getClient();
            String client = c.getCliRaisonSocial();
            if (client == null || client.isBlank()) client = ((c.getCliPrenom() == null ? "" : c.getCliPrenom()) + " " + (c.getCliNom() == null ? "" : c.getCliNom())).trim();
            dto.setClientLibelle(client);
        }
        planningRepo.findByDevisIdAndDeletedFalse(devis.getId()).stream()
                .filter(p -> !p.isCloture()).min(Comparator.comparing(PlanningTravaux::getDateDebut))
                .ifPresent(p -> { dto.setPlanningDateDebut(p.getDateDebut()); dto.setPlanningDateFin(p.getDateFin()); });
        return dto;
    }

    private boolean overlaps(LocalDateTime aStart, LocalDateTime aEnd, LocalDateTime bStart, LocalDateTime bEnd) {
        return aStart.isBefore(bEnd) && bStart.isBefore(aEnd);
    }

    private LocalDateTime effectiveEnd(PlanningTravaux p) {
        return p.getDateFin() != null && p.getDateFin().isAfter(p.getDateDebut()) ? p.getDateFin() : p.getDateDebut().plusHours(8);
    }

    private void ensureNoTerminatedPlanning(String devisId) {
        boolean locked = planningRepo.findByDevisId(devisId).stream()
                .anyMatch(p -> p.isDeleted() || p.isCloture() || p.getDateFin() != null);
        if (locked) throw new ConflictException("Le devis contient un planning terminÃ© et ses affectations sont en lecture seule.");
    }

    private String fullName(Agent a) { return (a.getPrenom() + " " + a.getNom()).trim(); }
}
