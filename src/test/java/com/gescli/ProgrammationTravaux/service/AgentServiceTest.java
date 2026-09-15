package com.gescli.ProgrammationTravaux.service;

import com.gescli.ProgrammationTravaux.dto.AgentResponseDTO;
import com.gescli.ProgrammationTravaux.entity.Agent;
import com.gescli.ProgrammationTravaux.entity.PlanningTravaux;
import com.gescli.ProgrammationTravaux.mapper.AgentMapper;
import com.gescli.ProgrammationTravaux.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentServiceTest {
    @Mock AgentRepository agentRepository;
    @Mock AgentMapper agentMapper;
    @Mock StructureRepository structureRepository;
    @Mock RoleRepository roleRepository;
    @Mock AffectationDevisRepository affectationRepository;
    @Mock PlanningTravauxRepository planningRepository;
    @Mock KeycloakAuthService keycloakAuthService;
    @Mock ActivityLogService activityLogService;
    @InjectMocks AgentService service;

    private Agent agent;

    @BeforeEach
    void setUp() {
        agent = new Agent();
        agent.setId("agent-1");
        agent.setNom("KABORE");
        agent.setPrenom("Ali");
        agent.setCode("A001");
        agent.setActif(true);
        when(agentRepository.findById("agent-1")).thenReturn(Optional.of(agent));
        when(agentMapper.toDto(agent)).thenReturn(new AgentResponseDTO());
    }

    @Test
    void shouldBeDisponibleWhenNoActiveAssignment() {
        when(affectationRepository.countByAgentIdAndActiveTrue("agent-1")).thenReturn(0L);
        when(planningRepository.findOpenForAgent("agent-1")).thenReturn(List.of());

        AgentResponseDTO result = service.getAgentById("agent-1");

        assertThat(result.getDisponibilite()).isEqualTo("DISPONIBLE");
        assertThat(result.getActiveAffectationsCount()).isZero();
    }

    @Test
    void shouldBeAffecteWhenAssignedButNotCurrentlyPlanned() {
        when(affectationRepository.countByAgentIdAndActiveTrue("agent-1")).thenReturn(2L);
        PlanningTravaux future = new PlanningTravaux();
        future.setDateDebut(LocalDateTime.now().plusDays(1));
        future.setDateFin(LocalDateTime.now().plusDays(1).plusHours(4));
        when(planningRepository.findOpenForAgent("agent-1")).thenReturn(List.of(future));

        AgentResponseDTO result = service.getAgentById("agent-1");

        assertThat(result.getDisponibilite()).isEqualTo("AFFECTE");
        assertThat(result.getActiveAffectationsCount()).isEqualTo(2);
    }

    @Test
    void shouldBeIndisponibleDuringCurrentPlanningWindow() {
        when(affectationRepository.countByAgentIdAndActiveTrue("agent-1")).thenReturn(1L);
        PlanningTravaux current = new PlanningTravaux();
        current.setDateDebut(LocalDateTime.now().minusHours(1));
        current.setDateFin(LocalDateTime.now().plusHours(2));
        when(planningRepository.findOpenForAgent("agent-1")).thenReturn(List.of(current));

        AgentResponseDTO result = service.getAgentById("agent-1");

        assertThat(result.getDisponibilite()).isEqualTo("INDISPONIBLE");
    }

    @Test
    void inactiveAgentAlwaysWinsOverComputedAvailability() {
        agent.setActif(false);
        when(affectationRepository.countByAgentIdAndActiveTrue("agent-1")).thenReturn(0L);

        AgentResponseDTO result = service.getAgentById("agent-1");

        assertThat(result.getDisponibilite()).isEqualTo("INACTIF");
    }
}
