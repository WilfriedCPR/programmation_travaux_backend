package com.gescli.ProgrammationTravaux.service;

import com.gescli.ProgrammationTravaux.dto.ActivityLogDTO;
import com.gescli.ProgrammationTravaux.entity.ActivityLog;
import com.gescli.ProgrammationTravaux.repository.ActivityLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ActivityLogService {
    private final ActivityLogRepository repository;

    @Transactional
    public void log(String type, String description, String reference, String devisId, String agentId) {
        ActivityLog log = new ActivityLog();
        log.setType(type);
        log.setDescription(description);
        log.setActeur(currentActor());
        log.setReference(reference);
        log.setDevisId(devisId);
        log.setAgentId(agentId);
        repository.save(log);
    }

    @Transactional(readOnly = true)
    public List<ActivityLogDTO> recent() {
        return repository.findTop100ByOrderByDateCreationDesc().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<ActivityLogDTO> byAgent(String agentId) {
        return repository.findTop50ByAgentIdOrderByDateCreationDesc(agentId).stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public String findActorAround(String agentId, String devisId, LocalDateTime instant, List<String> types) {
        if (agentId == null || devisId == null || instant == null || types == null || types.isEmpty()) return null;
        return repository.findFirstByAgentIdAndDevisIdAndTypeInAndDateCreationBetweenOrderByDateCreationAsc(
                        agentId, devisId, types, instant.minusMinutes(2), instant.plusMinutes(2))
                .map(ActivityLog::getActeur)
                .orElse(null);
    }

    public String currentActor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            String username = jwtAuth.getToken().getClaimAsString("preferred_username");
            if (username != null && !username.isBlank()) return username;
        }
        return auth != null ? auth.getName() : "SYSTEME";
    }

    private ActivityLogDTO toDto(ActivityLog e) {
        ActivityLogDTO dto = new ActivityLogDTO();
        dto.setId(e.getId());
        dto.setType(e.getType());
        dto.setDescription(e.getDescription());
        dto.setActeur(e.getActeur());
        dto.setReference(e.getReference());
        dto.setDevisId(e.getDevisId());
        dto.setAgentId(e.getAgentId());
        dto.setDateCreation(e.getDateCreation());
        return dto;
    }
}
