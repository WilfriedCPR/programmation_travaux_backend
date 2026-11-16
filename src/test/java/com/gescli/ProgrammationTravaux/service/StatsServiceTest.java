package com.gescli.ProgrammationTravaux.service;

import com.gescli.ProgrammationTravaux.entity.Agent;
import com.gescli.ProgrammationTravaux.entity.Devis;
import com.gescli.ProgrammationTravaux.entity.DevisStatut;
import com.gescli.ProgrammationTravaux.entity.PlanningTravaux;
import com.gescli.ProgrammationTravaux.entity.Travaux;
import com.gescli.ProgrammationTravaux.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatsServiceTest {
    @Mock DevisRepository devisRepo;
    @Mock PlanningTravauxRepository planningRepo;
    @Mock AffectationDevisRepository affectationRepo;
    @Mock AgentRepository agentRepo;
    @Mock BonSortieMaterielRepository bonSortieRepo;
    @Mock DemandeMaterielRepository demandeRepo;
    @Mock ParticipantRepository participantRepo;
    @Mock TravauxRepository travauxRepo;
    @Mock MaterielRepository materielRepo;
    @Mock TypeTravauxRepository typeTravauxRepo;
    @InjectMocks StatsService service;

    @AfterEach
    void clearSecurity() { SecurityContextHolder.clearContext(); }

    @Test
    void agentStatsUseOnlyVisiblePlanningsAndDevis() {
        Devis visible = devis("d-visible", DevisStatut.EN_COURS);
        PlanningTravaux planning = planning("p-visible", visible);
        when(planningRepo.findVisibleForAgent("agent-a")).thenReturn(List.of(planning));
        when(devisRepo.findAllById(List.of("d-visible"))).thenReturn(List.of(visible));
        when(bonSortieRepo.findByDemandeMateriel_Devis_Id("d-visible")).thenReturn(List.of());
        when(demandeRepo.findByDevisIdAndDeletedFalse("d-visible")).thenReturn(List.of());
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("agent-a", "", "ROLE_AGENT"));

        var stats = service.getStats();

        assertThat(stats.getTotalDevis()).isEqualTo(1);
        assertThat(stats.getTotalDevisEnCours()).isEqualTo(1);
        assertThat(stats.getDernieresActivites()).extracting("devisId").containsOnly("d-visible");
    }

    @Test
    void agentWithoutAssignmentGetsEmptyScopedCounters() {
        when(planningRepo.findVisibleForAgent("agent-empty")).thenReturn(List.of());
        when(devisRepo.findAllById(List.of())).thenReturn(List.of());
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("agent-empty", "", "ROLE_AGENT"));

        var stats = service.getStats();

        assertThat(stats.getTotalDevis()).isZero();
        assertThat(stats.getPlanningsSemaine()).isZero();
        assertThat(stats.getTravauxEnRetard()).isZero();
        assertThat(stats.getDernieresActivites()).isEmpty();
    }

    @Test
    void adminCatalogueCountersComeFromTheirOwnRepositories() {
        when(devisRepo.findTop5ByOrderByDateCreationDesc()).thenReturn(List.of());
        when(planningRepo.findTop5ByOrderByDateDebutDesc()).thenReturn(List.of());
        when(bonSortieRepo.findTop5ByOrderByDateSortieDesc()).thenReturn(List.of());
        when(agentRepo.count()).thenReturn(4L);
        when(travauxRepo.count()).thenReturn(5L);
        when(materielRepo.count()).thenReturn(3L);
        when(typeTravauxRepo.count()).thenReturn(2L);
        when(participantRepo.countByExterneTrue()).thenReturn(6L);
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("admin-a", "", "ROLE_ADMIN"));

        var stats = service.getStats();

        assertThat(stats.getAgentsCatalogueTotal()).isEqualTo(4);
        assertThat(stats.getTotalTravaux()).isEqualTo(5);
        assertThat(stats.getTotalMateriels()).isEqualTo(3);
        assertThat(stats.getTotalTypesTravaux()).isEqualTo(2);
        assertThat(stats.getParticipantsExternesTotal()).isEqualTo(6);
    }

    private Devis devis(String id, DevisStatut statut) {
        Devis devis = new Devis();
        devis.setId(id);
        devis.setDevisCode(id);
        devis.setStatut(statut);
        devis.setDateCreation(LocalDateTime.now());
        return devis;
    }

    private PlanningTravaux planning(String id, Devis devis) {
        PlanningTravaux planning = new PlanningTravaux();
        planning.setId(id);
        planning.setDevis(devis);
        planning.setDateDebut(LocalDateTime.now());
        Travaux travaux = new Travaux();
        travaux.setLibelle("Travail visible");
        planning.setTravaux(travaux);
        Agent agent = new Agent();
        agent.setId("agent-a");
        planning.setAgent(agent);
        return planning;
    }
}
