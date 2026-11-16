package com.gescli.ProgrammationTravaux.repository;

import com.gescli.ProgrammationTravaux.entity.*;
import com.gescli.ProgrammationTravaux.dto.AffectationRequestDTO;
import com.gescli.ProgrammationTravaux.mapper.AffectationMapperImpl;
import com.gescli.ProgrammationTravaux.service.AffectationService;
import com.gescli.ProgrammationTravaux.service.ActivityLogService;
import com.gescli.ProgrammationTravaux.service.NotificationService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@DataJpaTest
@ActiveProfiles("test")
@Import({AffectationService.class, AffectationMapperImpl.class})
class AffectationDevisRepositoryIntegrationTest {
    @Autowired AgentRepository agentRepository;
    @Autowired DevisRepository devisRepository;
    @Autowired AffectationDevisRepository repository;
    @Autowired AffectationService service;
    @Autowired EntityManager entityManager;
    @MockBean ActivityLogService activityLogService;
    @MockBean NotificationService notificationService;

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void removalThenReassignmentKeepsDistinctPersistedHistory(boolean useReactivateEndpoint) {
        Agent agent = new Agent(); agent.setId("a-cycle"); agent.setNom("TEST"); agent.setPrenom("Agent");
        agentRepository.saveAndFlush(agent);
        Devis devis = new Devis(); devis.setId("d-cycle"); devis.setDevisCode("DEV-CYCLE");
        devisRepository.saveAndFlush(devis);
        when(activityLogService.currentActor()).thenReturn("chef.test");
        AffectationRequestDTO request = new AffectationRequestDTO();
        request.setAgentId(agent.getId()); request.setDevisId(devis.getId());
        String originalId = service.affecterAgent(request).getId();
        service.retirerAgent(devis.getId(), agent.getId());
        entityManager.flush(); entityManager.clear();
        LocalDateTime removedAt = repository.findById(originalId).orElseThrow().getDateRetraitAffectation();

        if (useReactivateEndpoint) service.reactiverAgent(devis.getId(), agent.getId());
        else service.affecterAgent(request);
        entityManager.flush(); entityManager.clear();

        assertThat(repository.findByAgentIdOrderByDateAffectationDesc(agent.getId())).hasSize(2);
        assertThat(repository.findById(originalId)).get().satisfies(previous -> {
            assertThat(previous.isActive()).isFalse();
            assertThat(previous.getDateRetraitAffectation()).isEqualTo(removedAt).isNotNull();
            assertThat(previous.getRetirePar()).isEqualTo("chef.test");
        });
        assertThat(repository.findByAgentIdAndActiveTrue(agent.getId())).singleElement().satisfies(active -> {
            assertThat(active.getId()).isNotEqualTo(originalId);
            assertThat(active.getDateRetraitAffectation()).isNull();
        });
        var history = service.getHistoryByAgentId(agent.getId());
        assertThat(history).hasSize(2);
        assertThat(history.stream().filter(a -> a.isActive())).hasSize(1);
        assertThat(history.stream().filter(a -> !a.isActive())).hasSize(1);
    }

    @Test
    void activeQueriesAndHistoryMustReflectRemovalWithoutDeletingRow() {
        Agent agent = new Agent(); agent.setId("agent-int"); agent.setNom("TEST"); agent.setPrenom("Agent"); agent.setCode("T001"); agent.setActif(true);
        agentRepository.saveAndFlush(agent);

        Devis devis = new Devis(); devis.setId("devis-int"); devis.setDevisCode("DEV-INT"); devis.setDateCreation(LocalDateTime.now()); devis.setStatut(DevisStatut.EN_COURS);
        devisRepository.saveAndFlush(devis);

        AffectationDevis active = new AffectationDevis(); active.setAgent(agent); active.setDevis(devis); active.setDateAffectation(LocalDateTime.now().minusHours(2)); active.setActive(true); active.setAffectePar("admin");
        repository.saveAndFlush(active);

        assertThat(repository.countByAgentIdAndActiveTrue(agent.getId())).isEqualTo(1);
        active.setActive(false); active.setDateRetraitAffectation(LocalDateTime.now()); active.setRetirePar("chef");
        repository.saveAndFlush(active);

        assertThat(repository.countByAgentIdAndActiveTrue(agent.getId())).isZero();
        assertThat(repository.findByAgentIdOrderByDateAffectationDesc(agent.getId()))
                .singleElement().satisfies(saved -> {
                    assertThat(saved.isActive()).isFalse();
                    assertThat(saved.getRetirePar()).isEqualTo("chef");
                    assertThat(saved.getDateRetraitAffectation()).isNotNull();
                });
    }
}
