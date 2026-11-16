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
    @Autowired AffectationDevisRepository affectationRepository;

    @Test
    void shouldIdentifyAgentBusyInCurrentPlanningWindow() {
        Agent agent = new Agent(); agent.setId("busy-agent"); agent.setNom("BUSY"); agent.setPrenom("Agent"); agent.setCode("B001"); agent.setActif(true);
        agentRepository.saveAndFlush(agent);
        Devis devis = new Devis(); devis.setId("busy-devis"); devis.setDevisCode("DEV-BUSY"); devis.setDateCreation(LocalDateTime.now()); devis.setStatut(DevisStatut.EN_COURS);
        devisRepository.saveAndFlush(devis);
        Travaux travaux = new Travaux(); travaux.setLibelle("Travaux test"); travaux = travauxRepository.saveAndFlush(travaux);
        AffectationDevis affectation = new AffectationDevis(); affectation.setAgent(agent); affectation.setDevis(devis); affectation.setDateAffectation(LocalDateTime.now().minusDays(1)); affectation.setActive(true);
        affectationRepository.saveAndFlush(affectation);
        PlanningTravaux p = new PlanningTravaux(); p.setDevis(devis); p.setTravaux(travaux); p.setAgent(agent); p.setDateDebut(LocalDateTime.now().minusMinutes(30)); p.setDateFin(null); p.setDeleted(false); p.setCloture(false);
        planningRepository.saveAndFlush(p);

        assertThat(planningRepository.findOpenForAgent(agent.getId())).hasSize(1);
        assertThat(planningRepository.countAgentsCurrentlyBusy(LocalDateTime.now())).isEqualTo(1);
    }

    @Test
    void stalePlanningIsIgnoredWhenAssignmentIsNoLongerActive() {
        Agent agent = new Agent(); agent.setId("stale-agent"); agent.setNom("STALE"); agent.setPrenom("Agent"); agent.setCode("S001"); agent.setActif(true);
        agentRepository.saveAndFlush(agent);
        Devis devis = new Devis(); devis.setId("stale-devis"); devis.setDevisCode("DEV-STALE"); devis.setDateCreation(LocalDateTime.now()); devis.setStatut(DevisStatut.EN_COURS);
        devisRepository.saveAndFlush(devis);
        Travaux travaux = new Travaux(); travaux.setLibelle("Travaux historique"); travaux = travauxRepository.saveAndFlush(travaux);
        AffectationDevis affectation = new AffectationDevis(); affectation.setAgent(agent); affectation.setDevis(devis); affectation.setDateAffectation(LocalDateTime.now().minusDays(2)); affectation.setDateRetraitAffectation(LocalDateTime.now().minusDays(1)); affectation.setActive(false);
        affectationRepository.saveAndFlush(affectation);
        PlanningTravaux p = new PlanningTravaux(); p.setDevis(devis); p.setTravaux(travaux); p.setAgent(agent); p.setDateDebut(LocalDateTime.now().minusMinutes(30)); p.setDateFin(LocalDateTime.now().plusHours(2)); p.setDeleted(false); p.setCloture(false);
        planningRepository.saveAndFlush(p);

        assertThat(planningRepository.findOpenForAgent(agent.getId())).isEmpty();
        assertThat(planningRepository.countAgentsCurrentlyBusy(LocalDateTime.now())).isZero();
    }
    @Test
    void finishedPlanningNoLongerBlocksAgent() {
        Agent agent = new Agent(); agent.setId("finished-agent"); agent.setNom("FINISHED"); agent.setPrenom("Agent"); agent.setCode("F001"); agent.setActif(true);
        agentRepository.saveAndFlush(agent);
        Devis devis = new Devis(); devis.setId("finished-devis"); devis.setDevisCode("DEV-FINISHED"); devis.setDateCreation(LocalDateTime.now()); devis.setStatut(DevisStatut.EN_COURS);
        devisRepository.saveAndFlush(devis);
        Travaux travaux = new Travaux(); travaux.setLibelle("Travaux terminé"); travaux = travauxRepository.saveAndFlush(travaux);
        AffectationDevis affectation = new AffectationDevis(); affectation.setAgent(agent); affectation.setDevis(devis); affectation.setDateAffectation(LocalDateTime.now().minusDays(1)); affectation.setActive(true);
        affectationRepository.saveAndFlush(affectation);
        PlanningTravaux p = new PlanningTravaux(); p.setDevis(devis); p.setTravaux(travaux); p.setAgent(agent); p.setDateDebut(LocalDateTime.now().minusHours(3)); p.setDateFin(LocalDateTime.now().minusHours(1)); p.setDeleted(false); p.setCloture(false);
        planningRepository.saveAndFlush(p);

        assertThat(planningRepository.findOpenForAgent(agent.getId())).isEmpty();
    }

}
