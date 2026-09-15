package com.gescli.ProgrammationTravaux.service;

import com.gescli.ProgrammationTravaux.dto.AgentRequestDTO;
import com.gescli.ProgrammationTravaux.dto.AgentResponseDTO;
import com.gescli.ProgrammationTravaux.entity.Agent;
import com.gescli.ProgrammationTravaux.entity.Role;
import com.gescli.ProgrammationTravaux.entity.Structure;
import com.gescli.ProgrammationTravaux.mapper.AgentMapper;
import com.gescli.ProgrammationTravaux.repository.AffectationDevisRepository;
import com.gescli.ProgrammationTravaux.repository.AgentRepository;
import com.gescli.ProgrammationTravaux.repository.RoleRepository;
import com.gescli.ProgrammationTravaux.repository.PlanningTravauxRepository;
import com.gescli.ProgrammationTravaux.repository.StructureRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AgentService {

    private final AgentRepository agentRepository;
    private final AgentMapper agentMapper;
    private final StructureRepository structureRepository;
    private final RoleRepository roleRepository;
    private final AffectationDevisRepository affectationRepository;
    private final PlanningTravauxRepository planningRepository;
    private final KeycloakAuthService keycloakAuthService;
    private final ActivityLogService activityLogService;

    @Transactional
    public AgentResponseDTO createAgent(AgentRequestDTO dto) {
        if (dto.getPassword() == null || dto.getPassword().isBlank()) {
            throw new IllegalArgumentException("Le mot de passe est obligatoire");
        }
        if (agentRepository.findByCode(dto.getCode().trim()).isPresent()) {
            throw new IllegalArgumentException("Ce matricule existe déjà");
        }

        String roleName = canonicalRole(dto.getRoleName());
        Role role = resolveRole(roleName);
        Structure structure = sonabel();
        String keycloakId = null;

        try {
            keycloakId = keycloakAuthService.createKeycloakUser(
                    dto.getCode().trim(), dto.getPassword(), dto.getPrenom().trim(), dto.getNom().trim(), roleName);

            Agent agent = agentMapper.toEntity(dto);
            agent.setId(keycloakId);
            agent.setNom(dto.getNom().trim());
            agent.setPrenom(dto.getPrenom().trim());
            agent.setCode(dto.getCode().trim());
            agent.setActif(true);
            agent.setStructure(structure);
            agent.getRoles().clear();
            agent.addRole(role);
            Agent saved = agentRepository.save(agent);
            activityLogService.log("AGENT_CREATION", "Création de l'agent " + fullName(saved), saved.getCode(), null, saved.getId());
            return toResponse(saved);
        } catch (Exception e) {
            if (keycloakId != null) keycloakAuthService.deleteUser(keycloakId);
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public AgentResponseDTO getAgentById(String id) { return toResponse(find(id)); }

    @Transactional(readOnly = true)
    public Optional<AgentResponseDTO> findAgentById(String id) {
        return agentRepository.findById(id).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<AgentResponseDTO> getAllAgents(Pageable pageable) {
        return agentRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional
    public AgentResponseDTO updateAgent(String id, AgentRequestDTO dto) {
        Agent agent = find(id);
        String previousRole = agent.getRoles().stream().findFirst().map(Role::getLibelle).orElse("");
        String roleName = canonicalRole(dto.getRoleName());
        Role role = resolveRole(roleName);
        Structure structure = sonabel();

        keycloakAuthService.updateUserIdentity(id, dto.getCode().trim(), dto.getPrenom().trim(), dto.getNom().trim());
        keycloakAuthService.replaceApplicationRole(id, roleName);

        agentMapper.updateAgentFromDto(dto, agent);
        agent.setNom(dto.getNom().trim());
        agent.setPrenom(dto.getPrenom().trim());
        agent.setCode(dto.getCode().trim());
        agent.setStructure(structure);
        agent.getRoles().clear();
        agent.addRole(role);

        Agent saved = agentRepository.save(agent);
        if (!previousRole.equalsIgnoreCase(roleName)) {
            activityLogService.log("ROLE_MODIFICATION",
                    "Rôle de " + fullName(saved) + " modifié : " + previousRole + " → " + roleName,
                    saved.getCode(), null, saved.getId());
        } else {
            activityLogService.log("AGENT_MODIFICATION", "Modification de l'agent " + fullName(saved), saved.getCode(), null, saved.getId());
        }
        keycloakAuthService.logoutUserSessions(id);
        return toResponse(saved);
    }

    @Transactional
    public void syncProfileFromKeycloak(String id, String prenom, String nom) {
        agentRepository.findById(id).ifPresent(agent -> {
            if (prenom != null) agent.setPrenom(prenom.trim());
            if (nom != null) agent.setNom(nom.trim());
            agent.setStructure(sonabel());
            agentRepository.save(agent);
        });
    }

    /** Ancien DELETE : conserve désormais l'historique et désactive le compte. */
    @Transactional
    public void deleteAgent(String id) { deactivateAgent(id); }

    @Transactional
    public AgentResponseDTO deactivateAgent(String id) {
        Agent agent = find(id);
        long activeAssignments = affectationRepository.countByAgentIdAndActiveTrue(id);
        if (activeAssignments > 0) {
            throw new IllegalStateException("Cet agent possède encore " + activeAssignments + " affectation(s) active(s). Retirez-les avant de le désactiver.");
        }
        if (!agent.isActif()) return toResponse(agent);
        keycloakAuthService.setUserEnabled(id, false);
        agent.setActif(false);
        Agent saved = agentRepository.save(agent);
        activityLogService.log("AGENT_DESACTIVATION", "Désactivation de l'agent " + fullName(saved), saved.getCode(), null, saved.getId());
        return toResponse(saved);
    }

    @Transactional
    public AgentResponseDTO reactivateAgent(String id) {
        Agent agent = find(id);
        if (agent.isActif()) return toResponse(agent);
        keycloakAuthService.setUserEnabled(id, true);
        agent.setActif(true);
        Agent saved = agentRepository.save(agent);
        activityLogService.log("AGENT_REACTIVATION", "Réactivation de l'agent " + fullName(saved), saved.getCode(), null, saved.getId());
        return toResponse(saved);
    }

    private AgentResponseDTO toResponse(Agent agent) {
        AgentResponseDTO dto = agentMapper.toDto(agent);
        long count = affectationRepository.countByAgentIdAndActiveTrue(agent.getId());
        dto.setActiveAffectationsCount((int) count);
        dto.setActif(agent.isActif());
        boolean currentlyBusy = agent.isActif() && planningRepository.findOpenForAgent(agent.getId()).stream()
                .anyMatch(p -> isCurrentlyRunning(p.getDateDebut(), p.getDateFin()));
        dto.setDisponibilite(!agent.isActif() ? "INACTIF" : currentlyBusy ? "INDISPONIBLE" : count > 0 ? "AFFECTE" : "DISPONIBLE");
        return dto;
    }

    private Agent find(String id) {
        return agentRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Agent introuvable : " + id));
    }

    private Structure sonabel() {
        return structureRepository.findFirstByLibelleIgnoreCase("SONABEL")
                .orElseGet(() -> { Structure s = new Structure(); s.setLibelle("SONABEL"); return structureRepository.save(s); });
    }

    private Role resolveRole(String roleName) {
        return roleRepository.findByLibelle(roleName).orElseThrow(() -> new EntityNotFoundException("Rôle introuvable : " + roleName));
    }

    private String canonicalRole(String roleName) {
        String value = roleName == null ? "" : roleName.trim().toUpperCase();
        if (value.contains("ADMIN")) return "ADMIN";
        if (value.contains("CHEF")) return "CHEF";
        if (value.contains("AGENT")) return "AGENT";
        throw new IllegalArgumentException("Rôle invalide : " + roleName);
    }

    private boolean isCurrentlyRunning(LocalDateTime start, LocalDateTime end) {
        if (start == null) return false;
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(start)) return false;
        return end == null || !now.isAfter(end);
    }

    private String fullName(Agent a) { return (a.getPrenom() + " " + a.getNom()).trim(); }
}
