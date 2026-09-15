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
class PlanningTravauxRepositoryIntegrationTest {
    @Autowired AgentRepository agentRepository;
    @Autowired DevisRepository devisRepository;
    @Autowired TravauxRepository travauxRepository;
    @Autowired PlanningTravauxRepository planningRepository;

    @Test
    void shouldIdentifyAgentBusyInCurrentPlanningWindow() {
        Agent agent = new Agent(); agent.setId("busy-agent"); agent.setNom("BUSY"); agent.setPrenom("Agent"); agent.setCode("B001"); agent.setActif(true);
        agentRepository.saveAndFlush(agent);
        Devis devis = new Devis(); devis.setId("busy-devis"); devis.setDevisCode("DEV-BUSY"); devis.setDateCreation(LocalDateTime.now()); devis.setStatut(DevisStatut.EN_COURS);
        devisRepository.saveAndFlush(devis);
        Travaux travaux = new Travaux(); travaux.setLibelle("Travaux test"); travaux = travauxRepository.saveAndFlush(travaux);
        PlanningTravaux p = new PlanningTravaux(); p.setDevis(devis); p.setTravaux(travaux); p.setAgent(agent); p.setDateDebut(LocalDateTime.now().minusMinutes(30)); p.setDateFin(LocalDateTime.now().plusHours(2)); p.setDeleted(false); p.setCloture(false);
        planningRepository.saveAndFlush(p);

        assertThat(planningRepository.findOpenForAgent(agent.getId())).hasSize(1);
        assertThat(planningRepository.countAgentsCurrentlyBusy(LocalDateTime.now())).isEqualTo(1);
    }
}
