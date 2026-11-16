package com.gescli.ProgrammationTravaux.service;

import com.gescli.ProgrammationTravaux.dto.AffectationRequestDTO;
import com.gescli.ProgrammationTravaux.dto.AffectationResponseDTO;
import com.gescli.ProgrammationTravaux.dto.ActivityLogDTO;
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


    @Test
    void removingAssignmentAlsoRemovesAgentFromOpenPlanningButKeepsClosedHistory() {
        Agent agent = agent("a1");
        Devis devis = devis("d1", "DEV-001");
        AffectationDevis aff = new AffectationDevis();
        aff.setId("aff-1"); aff.setAgent(agent); aff.setDevis(devis); aff.setActive(true); aff.setDateAffectation(LocalDateTime.now().minusDays(2));

        PlanningTravaux open = planning("p-open", devis, new Travaux(), LocalDateTime.now().minusHours(1), LocalDateTime.now().plusHours(2));
        open.setAgent(agent);
        open.getParticipants().add(agent);
        PlanningTravaux closed = planning("p-closed", devis, new Travaux(), LocalDateTime.now().minusDays(2), LocalDateTime.now().minusDays(2).plusHours(2));
        closed.setCloture(true);
        closed.setAgent(agent);
        closed.getParticipants().add(agent);

        when(affectationRepo.findByAgentIdAndDevisIdAndActiveTrue("a1", "d1")).thenReturn(Optional.of(aff));
        when(planningRepo.findByDevisIdAndDeletedFalse("d1")).thenReturn(List.of(open, closed));

        service.retirerAgent("d1", "a1");

        assertThat(open.getAgent()).isNull();
        assertThat(open.getParticipants()).doesNotContain(agent);
        assertThat(closed.getAgent()).isSameAs(agent);
        assertThat(closed.getParticipants()).contains(agent);
        verify(planningRepo).save(open);
        verify(planningRepo, never()).save(closed);
    }


    @Test
    void reactivatingAssignmentCreatesNewRowAndPreservesPreviousHistory() {
        Agent agent = agent("a1");
        Devis devis = devis("d1", "DEV-001");
        AffectationDevis previous = new AffectationDevis();
        previous.setId("aff-old");
        previous.setAgent(agent);
        previous.setDevis(devis);
        previous.setActive(false);
        previous.setDateAffectation(LocalDateTime.now().minusDays(5));
        previous.setDateRetraitAffectation(LocalDateTime.now().minusDays(2));
        previous.setAffectePar("chef.old");
        previous.setRetirePar("admin.old");

        when(agentRepo.findById("a1")).thenReturn(Optional.of(agent));
        when(affectationRepo.findByAgentIdAndDevisIdAndActiveTrue("a1", "d1")).thenReturn(Optional.empty());
        when(affectationRepo.findTopByAgentIdAndDevisIdAndActiveFalseOrderByDateAffectationDesc("a1", "d1"))
                .thenReturn(Optional.of(previous));
        when(activityLogService.currentActor()).thenReturn("chef.new");

        service.reactiverAgent("d1", "a1");

        // L'ancienne ligne reste strictement historique.
        assertThat(previous.isActive()).isFalse();
        assertThat(previous.getDateRetraitAffectation()).isNotNull();
        assertThat(previous.getRetirePar()).isEqualTo("admin.old");

        verify(affectationRepo).save(argThat(saved ->
                saved != previous
                        && saved.isActive()
                        && saved.getAgent() == agent
                        && saved.getDevis() == devis
                        && saved.getDateAffectation() != null
                        && saved.getDateRetraitAffectation() == null
                        && "chef.new".equals(saved.getAffectePar())
                        && saved.getRetirePar() == null));
    }

    @Test
    void inactiveHistoryRecoversLegacyRemovalFromActivityLog() {
        Agent agent = agent("a1");
        Devis devis = devis("d1", "DEV-001");
        LocalDateTime removedAt = LocalDateTime.now().minusDays(1);
        ActivityLogDTO removal = new ActivityLogDTO();
        removal.setId("log-r");
        removal.setType("AFFECTATION_RETRAIT");
        removal.setAgentId("a1");
        removal.setDevisId("d1");
        removal.setReference("DEV-001");
        removal.setActeur("admin.old");
        removal.setDateCreation(removedAt);
        ActivityLogDTO addition = new ActivityLogDTO();
        addition.setId("log-a");
        addition.setType("AFFECTATION_AJOUT");
        addition.setAgentId("a1");
        addition.setDevisId("d1");
        addition.setActeur("chef.old");
        addition.setDateCreation(removedAt.minusDays(2));

        when(agentRepo.existsById("a1")).thenReturn(true);
        when(affectationRepo.findByAgentIdAndActiveFalseOrderByDateRetraitAffectationDesc("a1"))
                .thenReturn(List.of());
        when(activityLogService.historyEventsByAgent(eq("a1"), anyList())).thenReturn(List.of(removal));
        when(activityLogService.latestEventBefore(eq("a1"), eq("d1"), anyList(), eq(removedAt)))
                .thenReturn(addition);
        when(devisRepo.findById("d1")).thenReturn(Optional.of(devis));

        List<AffectationResponseDTO> history = service.getInactiveHistoryByAgentId("a1");

        assertThat(history).hasSize(1);
        assertThat(history.get(0).isActive()).isFalse();
        assertThat(history.get(0).getDevisCode()).isEqualTo("DEV-001");
        assertThat(history.get(0).getDateRetraitAffectation()).isEqualTo(removedAt);
        assertThat(history.get(0).getAffectePar()).isEqualTo("chef.old");
        assertThat(history.get(0).getRetirePar()).isEqualTo("admin.old");
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
