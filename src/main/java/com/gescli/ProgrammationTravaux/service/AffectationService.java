package com.gescli.ProgrammationTravaux.service;

import com.gescli.ProgrammationTravaux.dto.AffectationRequestDTO;
import com.gescli.ProgrammationTravaux.dto.AffectationResponseDTO;
import com.gescli.ProgrammationTravaux.entity.AffectationDevis;
import com.gescli.ProgrammationTravaux.entity.Agent;
import com.gescli.ProgrammationTravaux.entity.Devis;
import com.gescli.ProgrammationTravaux.mapper.AffectationMapper;
import com.gescli.ProgrammationTravaux.repository.AffectationDevisRepository;
import com.gescli.ProgrammationTravaux.repository.AgentRepository;
import com.gescli.ProgrammationTravaux.repository.DevisRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AffectationService {

    private final AffectationDevisRepository affectationRepo;
    private final AgentRepository agentRepo;
    private final DevisRepository devisRepo;
    private final AffectationMapper mapper;

    @Transactional
    public AffectationResponseDTO affecterAgent(AffectationRequestDTO dto) {
        Agent agent = agentRepo.findById(dto.getAgentId())
                .orElseThrow(() -> new EntityNotFoundException("Agent introuvable : " + dto.getAgentId()));
        Devis devis = devisRepo.findById(dto.getDevisId())
                .orElseThrow(() -> new EntityNotFoundException("Devis introuvable : " + dto.getDevisId()));

        if (affectationRepo.findByAgentIdAndDevisIdAndActiveTrue(dto.getAgentId(), dto.getDevisId()).isPresent()) {
            throw new IllegalStateException("L'agent est déjà affecté à ce devis.");
        }

        AffectationDevis affectation = new AffectationDevis();
        affectation.setAgent(agent);
        affectation.setDevis(devis);
        affectation.setDateAffectation(LocalDateTime.now());
        affectation.setActive(true);

        AffectationDevis saved = affectationRepo.save(affectation);
        AffectationResponseDTO response = mapper.toDto(saved);

        List<String> dejaAffectes = affectationRepo.findByAgentIdAndActiveTrue(dto.getAgentId())
                .stream()
                .map(a -> a.getDevis().getDevisCode())
                .collect(Collectors.toList());
        response.setDevisDejaAffectes(dejaAffectes);
        return response;
    }

    @Transactional
    public void retirerAgent(String devisId, String agentId) {
        AffectationDevis aff = affectationRepo.findByAgentIdAndDevisIdAndActiveTrue(agentId, devisId)
                .orElseThrow(() -> new EntityNotFoundException("Affectation active introuvable pour agent=" + agentId + ", devis=" + devisId));
        aff.setActive(false);
        aff.setDateRetraitAffectation(LocalDateTime.now());
        affectationRepo.save(aff);
    }

    @Transactional
    public void reactiverAgent(String devisId, String agentId) {
        AffectationDevis aff = affectationRepo.findByAgentIdAndDevisIdAndActiveFalse(agentId, devisId)
                .orElseThrow(() -> new EntityNotFoundException("Affectation inactive introuvable pour agent=" + agentId + ", devis=" + devisId));
        aff.setActive(true);
        aff.setDateRetraitAffectation(null);
        affectationRepo.save(aff);
    }

    @Transactional(readOnly = true)
    public List<AffectationResponseDTO> getAgentsByDevisId(String devisId) {
        return affectationRepo.findByDevisId(devisId).stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AffectationResponseDTO> getDevisByAgentId(String agentId) {
        return affectationRepo.findByAgentIdAndActiveTrue(agentId).stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }
}
