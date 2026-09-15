package com.gescli.ProgrammationTravaux.service;

import com.gescli.ProgrammationTravaux.dto.AffectationRequestDTO;
import com.gescli.ProgrammationTravaux.entity.*;
import com.gescli.ProgrammationTravaux.mapper.AffectationMapper;
import com.gescli.ProgrammationTravaux.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AffectationServiceTest {
    @Mock AffectationDevisRepository affectationRepo;
    @Mock AgentRepository agentRepo;
    @Mock DevisRepository devisRepo;
    @Mock PlanningTravauxRepository planningRepo;
    @Mock AffectationMapper mapper;
    @Mock ActivityLogService activityLogService;
    @Mock NotificationService notificationService;
    @InjectMocks AffectationService service;

    @Test
    void shouldBlockConflictingAssignmentUntilExplicitlyForced() {
        Agent agent = agent("a1");
        Devis targetDevis = devis("d1", "DEV-001");
        Devis otherDevis = devis("d2", "DEV-002");
        Travaux travaux = new Travaux(); travaux.setLibelle("Pose réseau");

        PlanningTravaux target = planning("p1", targetDevis, travaux, LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(1).plusHours(4));
        PlanningTravaux conflict = planning("p2", otherDevis, travaux, LocalDateTime.now().plusDays(1).plusHours(1), LocalDateTime.now().plusDays(1).plusHours(5));

        when(agentRepo.findById("a1")).thenReturn(Optional.of(agent));
        when(devisRepo.findById("d1")).thenReturn(Optional.of(targetDevis));
        when(affectationRepo.findByAgentIdAndDevisIdAndActiveTrue("a1", "d1")).thenReturn(Optional.empty());
        when(planningRepo.findByDevisIdAndDeletedFalse("d1")).thenReturn(List.of(target));
        when(planningRepo.findOpenForAgent("a1")).thenReturn(List.of(target, conflict));

        AffectationRequestDTO request = new AffectationRequestDTO();
        request.setAgentId("a1"); request.setDevisId("d1"); request.setForce(false);

        assertThatThrownBy(() -> service.affecterAgent(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Conflit de planning");
        verify(affectationRepo, never()).save(any());
        verify(notificationService, never()).notifyAgent(anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void removingAssignmentKeepsHistoryAndTracksActor() {
        Agent agent = agent("a1");
        Devis devis = devis("d1", "DEV-001");
        AffectationDevis aff = new AffectationDevis();
        aff.setId("aff-1"); aff.setAgent(agent); aff.setDevis(devis); aff.setActive(true); aff.setDateAffectation(LocalDateTime.now().minusDays(2));
        when(affectationRepo.findByAgentIdAndDevisIdAndActiveTrue("a1", "d1")).thenReturn(Optional.of(aff));
        when(activityLogService.currentActor()).thenReturn("admin.test");

        service.retirerAgent("d1", "a1");

        assertThat(aff.isActive()).isFalse();
        assertThat(aff.getDateRetraitAffectation()).isNotNull();
        assertThat(aff.getRetirePar()).isEqualTo("admin.test");
        verify(affectationRepo).save(aff);
        verify(activityLogService).log(eq("AFFECTATION_RETRAIT"), anyString(), eq("DEV-001"), eq("d1"), eq("a1"));
    }

    private Agent agent(String id) {
        Agent a = new Agent(); a.setId(id); a.setNom("KABORE"); a.setPrenom("Ali"); a.setActif(true); return a;
    }
    private Devis devis(String id, String code) {
        Devis d = new Devis(); d.setId(id); d.setDevisCode(code); d.setDateCreation(LocalDateTime.now()); d.setStatut(DevisStatut.EN_COURS); return d;
    }
    private PlanningTravaux planning(String id, Devis devis, Travaux travaux, LocalDateTime start, LocalDateTime end) {
        PlanningTravaux p = new PlanningTravaux(); p.setId(id); p.setDevis(devis); p.setTravaux(travaux); p.setDateDebut(start); p.setDateFin(end); p.setDeleted(false); p.setCloture(false); return p;
    }
}
