package com.gescli.ProgrammationTravaux.service;

import com.gescli.ProgrammationTravaux.dto.TravauxContrainteRequestDTO;
import com.gescli.ProgrammationTravaux.dto.TravauxContrainteResponseDTO;
import com.gescli.ProgrammationTravaux.entity.*;
import com.gescli.ProgrammationTravaux.mapper.TravauxContrainteMapper;
import com.gescli.ProgrammationTravaux.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TravauxContrainteServiceTest {

    @Mock TravauxContrainteRepository repository;
    @Mock TravauxRepository travauxRepository;
    @Mock PlanningTravauxRepository planningRepository;
    @Mock TravauxContrainteMapper mapper;
    @Mock ContrainteRepository contrainteRepository;
    @InjectMocks TravauxContrainteService service;

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void agentAffectePeutAjouterEtLever() {
        PlanningTravaux planning = planning("p1", "agent-1");
        Contrainte contrainte = new Contrainte();
        contrainte.setLibelle("Accès");
        TravauxContrainte saved = new TravauxContrainte();
        saved.setId("c1");
        saved.setPlanning(planning);
        saved.setActive(true);
        when(planningRepository.findById("p1")).thenReturn(Optional.of(planning));
        when(contrainteRepository.findFirstByLibelleIgnoreCase("Accès")).thenReturn(Optional.of(contrainte));
        when(repository.save(any(TravauxContrainte.class))).thenReturn(saved);
        when(repository.findById("c1")).thenReturn(Optional.of(saved));
        when(mapper.toDto(saved)).thenReturn(new TravauxContrainteResponseDTO());
        agent("agent-1");

        service.addContrainteToPlanning("p1", "Accès", "Observation de mise");
        service.leverContrainte("p1", "c1", "Observation de levée");

        assertFalse(Boolean.TRUE.equals(saved.getActive()));
        assertNotNull(saved.getDateLeveeContrainte());
        assertEquals("Observation de levée", saved.getObservationLevee());
        verify(repository, times(2)).save(any(TravauxContrainte.class));
    }

    @Test
    void agentNonAffecteNePeutNiConsulterNiAjouter() {
        PlanningTravaux planning = planning("p1", "agent-autre");
        when(planningRepository.findById("p1")).thenReturn(Optional.of(planning));
        agent("agent-courant");

        assertThrows(AccessDeniedException.class, () -> service.getContraintesByPlanning("p1"));
        assertThrows(AccessDeniedException.class,
                () -> service.addContrainteToPlanning("p1", "Accès", "Observation"));
        verifyNoInteractions(repository);
    }

    @Test
    void agentNePeutPasSupprimerEtAdminPeutSupprimerUneContrainteLevee() {
        PlanningTravaux planning = planning("p1", "agent-1");
        TravauxContrainte entity = new TravauxContrainte();
        entity.setId("c1");
        entity.setPlanning(planning);
        entity.setActive(false);
        when(repository.findById("c1")).thenReturn(Optional.of(entity));

        agent("agent-1");
        assertThrows(AccessDeniedException.class, () -> service.deleteContrainte("c1"));
        verify(repository, never()).delete(any());

        admin();
        service.deleteContrainte("c1");
        verify(repository).delete(entity);
    }

    @Test
    void suppressionDuneContrainteActiveEstRefusee() {
        TravauxContrainte entity = new TravauxContrainte();
        entity.setId("c1");
        entity.setActive(true);
        when(repository.findById("c1")).thenReturn(Optional.of(entity));
        admin();

        assertThrows(IllegalStateException.class, () -> service.deleteContrainte("c1"));
        verify(repository, never()).delete(any());
    }

    private PlanningTravaux planning(String id, String assignedAgentId) {
        Agent agent = new Agent();
        agent.setId(assignedAgentId);
        Devis devis = new Devis();
        devis.setId("d1");
        AffectationDevis affectation = new AffectationDevis();
        affectation.setAgent(agent);
        affectation.setDevis(devis);
        affectation.setActive(true);
        devis.getAffectations().add(affectation);
        Travaux travaux = new Travaux();
        travaux.setId("t1");
        PlanningTravaux planning = new PlanningTravaux();
        planning.setId(id);
        planning.setAgent(agent);
        planning.setDevis(devis);
        planning.setTravaux(travaux);
        return planning;
    }

    private void agent(String id) {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken(id, "", "ROLE_AGENT"));
    }

    private void admin() {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("admin", "", "ROLE_ADMIN"));
    }
}
