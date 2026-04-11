package com.gescli.ProgrammationTravaux.service;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gescli.ProgrammationTravaux.dto.AgentRequestDTO;
import com.gescli.ProgrammationTravaux.dto.AgentResponseDTO;
import com.gescli.ProgrammationTravaux.entity.Agent;
import com.gescli.ProgrammationTravaux.entity.Role;
import com.gescli.ProgrammationTravaux.entity.Structure;
import com.gescli.ProgrammationTravaux.mapper.AgentMapper;
import com.gescli.ProgrammationTravaux.repository.AgentRepository;
import com.gescli.ProgrammationTravaux.repository.RoleRepository;
import com.gescli.ProgrammationTravaux.repository.StructureRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AgentService {

    private final AgentRepository agentRepository;
    private final AgentMapper agentMapper;
    private final StructureRepository structureRepository;
    private final RoleRepository roleRepository;
    private final KeycloakAuthService keycloakAuthService;

    @Transactional
    public AgentResponseDTO createAgent(AgentRequestDTO requestDTO) {
        String keycloakId = keycloakAuthService.createKeycloakUser(
            requestDTO.getCode(),
            requestDTO.getPassword(),
            requestDTO.getPrenom(),
            requestDTO.getNom(),
            requestDTO.getRoleName()
        );

        Agent agent = agentMapper.toEntity(requestDTO);
        agent.setId(keycloakId);

        if (requestDTO.getStructureId() != null && !requestDTO.getStructureId().trim().isEmpty()) {
            Structure structure = structureRepository.findById(requestDTO.getStructureId())
                .orElseThrow(() -> new EntityNotFoundException("Structure with ID " + requestDTO.getStructureId() + " not found."));
            agent.setStructure(structure);
        }

        if (requestDTO.getRoleName() != null && !requestDTO.getRoleName().trim().isEmpty()) {
            Role role = roleRepository.findByLibelle(requestDTO.getRoleName())
                .orElseThrow(() -> new EntityNotFoundException("Role with name " + requestDTO.getRoleName() + " not found."));
            agent.addRole(role);
        }

        Agent savedAgent = agentRepository.save(agent);
        return agentMapper.toDto(savedAgent);
    }

    public AgentResponseDTO getAgentById(String id) {
        Agent agent = agentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Agent with ID " + id + " not found."));
        return agentMapper.toDto(agent);
    }

    public Page<AgentResponseDTO> getAllAgents(Pageable pageable) {
        return agentRepository.findAll(pageable).map(agentMapper::toDto);
    }

    @Transactional
    public AgentResponseDTO updateAgent(String id, AgentRequestDTO requestDTO) {
        Agent existingAgent = agentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Agent with ID " + id + " not found."));
        agentMapper.updateAgentFromDto(requestDTO, existingAgent);

        if (requestDTO.getStructureId() != null) {
            Structure structure = structureRepository.findById(requestDTO.getStructureId())
                .orElseThrow(() -> new EntityNotFoundException("Structure with ID " + requestDTO.getStructureId() + " not found."));
            existingAgent.setStructure(structure);
        } else {
            existingAgent.setStructure(null);
        }

        if (requestDTO.getRoleName() != null && !requestDTO.getRoleName().trim().isEmpty()) {
            existingAgent.getRoles().clear();
            Role role = roleRepository.findByLibelle(requestDTO.getRoleName())
                .orElseThrow(() -> new EntityNotFoundException("Role with name " + requestDTO.getRoleName() + " not found."));
            existingAgent.addRole(role);
        } else {
            existingAgent.getRoles().clear();
        }

        return agentMapper.toDto(agentRepository.save(existingAgent));
    }

    @Transactional
    public void deleteAgent(String id) {
        Agent agent = agentRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Agent with ID " + id + " not found."));

        if (agent.getAffectations() != null) {
            agent.getAffectations().forEach(affectation -> {
                affectation.setAgent(null);
                affectation.setActive(false);
                affectation.setDateRetraitAffectation(LocalDateTime.now());
            });
        }

        try {
            keycloakAuthService.deleteUser(agent.getId());
        } catch (Exception e) {
            System.err.println("Warning: Could not delete user from Keycloak: " + e.getMessage());
        }

        agentRepository.deleteById(id);
    }
}
