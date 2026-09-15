package com.gescli.ProgrammationTravaux.repository;

import com.gescli.ProgrammationTravaux.entity.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class AffectationDevisRepositoryIntegrationTest {
    @Autowired AgentRepository agentRepository;
    @Autowired DevisRepository devisRepository;
    @Autowired AffectationDevisRepository repository;

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
