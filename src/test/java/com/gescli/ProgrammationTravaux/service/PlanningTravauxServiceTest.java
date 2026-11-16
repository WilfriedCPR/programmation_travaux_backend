package com.gescli.ProgrammationTravaux.service;

import com.gescli.ProgrammationTravaux.dto.PlanningTravauxRequestDTO;
import com.gescli.ProgrammationTravaux.entity.AffectationDevis;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlanningTravauxServiceTest {

    @Mock PlanningTravauxRepository planningRepo;
    @Mock DevisRepository devisRepo;
    @Mock TravauxRepository travauxRepo;
    @Mock TypeTravauxRepository typeTravauxRepo;
    @Mock AgentRepository agentRepo;
    @Mock ParticipantRepository participantRepo;
    @Mock TravauxContrainteRepository contrainteRepo;
    @Mock MaterielRepository materielRepository;
    @Mock DocumentRepository documentRepository;
    @Mock FileStorageService fileStorageService;
    @Mock ActivityLogService activityLogService;
    @Mock NotificationService notificationService;
    @InjectMocks PlanningTravauxService service;

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void agentAffectePeutRenseignerLaFin() {
        PlanningTravaux planning = planning("p1", "d1", "agent-1");
        when(planningRepo.findById("p1")).thenReturn(Optional.of(planning));
        when(contrainteRepo.countByPlanning_IdAndActiveTrue("p1")).thenReturn(0L);
        when(planningRepo.save(planning)).thenReturn(planning);
        agentAuthentication("agent-1");

        LocalDateTime end = planning.getDateDebut().plusHours(4);
        PlanningTravaux result = service.finishPlanning("p1", end);

        assertSame(planning, result);
        assertEquals(end, planning.getDateFin());
        verify(planningRepo).save(planning);
    }

    @Test
    void conservePlusieursPvDeMemeCategorie() throws Exception {
        PlanningTravaux planning = planning("p1", "d1", "agent-1");
        when(planningRepo.findById("p1")).thenReturn(Optional.of(planning));
        when(fileStorageService.store(any(), eq("pv"))).thenReturn("pv-1.pdf", "pv-2.pdf");
        when(documentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var first = new MockMultipartFile("file", "etape-1.pdf", "application/pdf", "one".getBytes());
        var second = new MockMultipartFile("file", "etape-2.pdf", "application/pdf", "two".getBytes());
        service.uploadPvDoc("p1", first, "ETAPE", null, null);
        service.uploadPvDoc("p1", second, "ETAPE", null, null);

        verify(documentRepository, times(2)).save(any());
        verify(fileStorageService, times(2)).store(any(), eq("pv"));
    }

    @Test
    void refuseUploadPvApresTerminaison() throws Exception {
        PlanningTravaux planning = planning("p1", "d1", "agent-1");
        planning.setDateFin(planning.getDateDebut().plusHours(2));
        when(planningRepo.findById("p1")).thenReturn(Optional.of(planning));

        var file = new MockMultipartFile("file", "etape.pdf", "application/pdf", "pdf".getBytes());
        assertThrows(IllegalStateException.class, () -> service.uploadPvDoc("p1", file, "ETAPE", null, null));
        verifyNoInteractions(fileStorageService, documentRepository);
    }

    @Test
    void refuseUploadPvSurPlanningSupprime() throws Exception {
        PlanningTravaux planning = planning("p1", "d1", "agent-1");
        planning.setDeleted(true);
        when(planningRepo.findById("p1")).thenReturn(Optional.of(planning));

        var file = new MockMultipartFile("file", "etape.pdf", "application/pdf", "pdf".getBytes());
        assertThrows(IllegalStateException.class, () -> service.uploadPvDoc("p1", file, "ETAPE", null, null));
        verifyNoInteractions(fileStorageService, documentRepository);
    }

    @Test
    void refuseUploadPvSurDevisCloture() throws Exception {
        PlanningTravaux planning = planning("p1", "d1", "agent-1");
        planning.getDevis().setStatut(DevisStatut.CLOS);
        when(planningRepo.findById("p1")).thenReturn(Optional.of(planning));

        var file = new MockMultipartFile("file", "etape.pdf", "application/pdf", "pdf".getBytes());
        assertThrows(IllegalStateException.class, () -> service.uploadPvDoc("p1", file, "ETAPE", null, null));
        verifyNoInteractions(fileStorageService, documentRepository);
    }

    @Test
    void agentNonAffecteNePeutPasTeleverserUnPv() {
        PlanningTravaux planning = planning("p1", "d1", "agent-affecte");
        when(planningRepo.findById("p1")).thenReturn(Optional.of(planning));
        agentAuthentication("agent-non-affecte");

        var file = new MockMultipartFile("file", "etape.pdf", "application/pdf", "pdf".getBytes());
        assertThrows(AccessDeniedException.class, () -> service.uploadPvDoc("p1", file, "ETAPE", null, null));
        verifyNoInteractions(fileStorageService, documentRepository);
    }

    @Test
    void agentAffectePeutTeleverserPlusieursPv() throws Exception {
        PlanningTravaux planning = planning("p1", "d1", "agent-1");
        when(planningRepo.findById("p1")).thenReturn(Optional.of(planning));
        when(fileStorageService.store(any(), eq("pv"))).thenReturn("pv-1.pdf", "pv-2.pdf");
        when(documentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        agentAuthentication("agent-1");

        service.uploadPvDoc("p1", new MockMultipartFile("file", "a.pdf", "application/pdf", "a".getBytes()), "ETAPE", null, null);
        service.uploadPvDoc("p1", new MockMultipartFile("file", "b.pdf", "application/pdf", "b".getBytes()), "ETAPE", null, null);

        verify(documentRepository, times(2)).save(any());
        verify(fileStorageService, times(2)).store(any(), eq("pv"));
    }

    @Test
    void refuseUneDateDeFinAnterieureAuDebut() {
        PlanningTravaux planning = planning("p1", "d1", "agent-1");
        when(planningRepo.findById("p1")).thenReturn(Optional.of(planning));

        assertThrows(IllegalArgumentException.class,
                () -> service.finishPlanning("p1", planning.getDateDebut().minusMinutes(1)));
        verify(planningRepo, never()).save(any());
    }

    @Test
    void accepteUneTerminaisonApresMinuit() {
        PlanningTravaux planning = planning("p1", "d1", "agent-1");
        when(planningRepo.findById("p1")).thenReturn(Optional.of(planning));
        when(contrainteRepo.countByPlanning_IdAndActiveTrue("p1")).thenReturn(0L);
        when(planningRepo.save(planning)).thenReturn(planning);
        agentAuthentication("agent-1");

        var nextDay = planning.getDateDebut().toLocalDate().plusDays(1).atStartOfDay().plusMinutes(30);
        assertDoesNotThrow(() -> service.finishPlanning("p1", nextDay));
        assertEquals(nextDay, planning.getDateFin());
    }

    @Test
    void nettoieLeFichierSiLaMetadonneePvNePeutPasEtreEnregistree() throws Exception {
        PlanningTravaux planning = planning("p1", "d1", "agent-1");
        Path stored = Files.createTempFile("pv-upload-", ".pdf");
        when(planningRepo.findById("p1")).thenReturn(Optional.of(planning));
        when(fileStorageService.store(any(), eq("pv"))).thenReturn(stored.toString());
        when(documentRepository.save(any())).thenThrow(new IllegalStateException("échec base"));

        var file = new MockMultipartFile("file", "etape.pdf", "application/pdf", "pdf".getBytes());
        assertThrows(IllegalStateException.class, () -> service.uploadPvDoc("p1", file, "ETAPE", null, null));
        assertFalse(Files.exists(stored));
    }

    @Test
    void agentNonAffecteNePeutPasRenseignerLaFin() {
        PlanningTravaux planning = planning("p1", "d1", "agent-autre");
        when(planningRepo.findById("p1")).thenReturn(Optional.of(planning));
        agentAuthentication("agent-courant");

        assertThrows(AccessDeniedException.class,
                () -> service.finishPlanning("p1", planning.getDateDebut().plusHours(4)));
        verify(planningRepo, never()).save(any());
    }

    @Test
    void agentAffectePeutModifierUnPlanningOuvert() {
        PlanningTravaux planning = planning("p1", "d1", "agent-1");
        Travaux travaux = planning.getTravaux();
        when(planningRepo.findById("p1")).thenReturn(Optional.of(planning));
        when(devisRepo.findById("d1")).thenReturn(Optional.of(planning.getDevis()));
        when(travauxRepo.findById("t1")).thenReturn(Optional.of(travaux));
        when(planningRepo.findOpenForAgent("agent-1")).thenReturn(java.util.List.of());
        when(planningRepo.save(planning)).thenReturn(planning);
        agentAuthentication("agent-1");

        PlanningTravauxRequestDTO dto = new PlanningTravauxRequestDTO();
        dto.setDevisId("d1");
        dto.setTravauxId("t1");
        dto.setDateDebut(planning.getDateDebut().plusHours(1));
        service.update("p1", dto);

        assertEquals(dto.getDateDebut(), planning.getDateDebut());
        verify(activityLogService).log(eq("PLANNING_MODIFICATION"), anyString(), eq("DV-1"), eq("d1"), eq("agent-1"));
        verify(planningRepo).save(planning);
    }

    @Test
    void agentNonAffecteNePeutPasModifier() {
        PlanningTravaux planning = planning("p1", "d1", "agent-autre");
        when(planningRepo.findById("p1")).thenReturn(Optional.of(planning));
        agentAuthentication("agent-courant");

        PlanningTravauxRequestDTO dto = new PlanningTravauxRequestDTO();
        dto.setDevisId("d1");
        dto.setTravauxId("t1");
        dto.setDateDebut(planning.getDateDebut());
        assertThrows(AccessDeniedException.class, () -> service.update("p1", dto));
        verify(planningRepo, never()).save(any());
    }

    @Test
    void agentNePeutPasChangerLeDevisNiModifierUnPlanningTermine() {
        PlanningTravaux planning = planning("p1", "d1", "agent-1");
        when(planningRepo.findById("p1")).thenReturn(Optional.of(planning));
        agentAuthentication("agent-1");
        PlanningTravauxRequestDTO dto = new PlanningTravauxRequestDTO();
        dto.setDevisId("d2");
        dto.setTravauxId("t1");
        dto.setDateDebut(planning.getDateDebut());
        assertThrows(AccessDeniedException.class, () -> service.update("p1", dto));

        planning.setDateFin(planning.getDateDebut().plusHours(2));
        dto.setDevisId("d1");
        assertThrows(IllegalStateException.class, () -> service.update("p1", dto));
    }

    @Test
    void adminPeutModifierUnPlanningOuvert() {
        PlanningTravaux planning = planning("p1", "d1", "agent-1");
        when(planningRepo.findById("p1")).thenReturn(Optional.of(planning));
        when(devisRepo.findById("d1")).thenReturn(Optional.of(planning.getDevis()));
        when(travauxRepo.findById("t1")).thenReturn(Optional.of(planning.getTravaux()));
        when(planningRepo.findOpenForAgent("agent-1")).thenReturn(java.util.List.of());
        when(planningRepo.save(planning)).thenReturn(planning);
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("admin", "", "ROLE_ADMIN"));
        PlanningTravauxRequestDTO dto = new PlanningTravauxRequestDTO();
        dto.setDevisId("d1");
        dto.setTravauxId("t1");
        dto.setDateDebut(planning.getDateDebut());
        service.update("p1", dto);
        verify(planningRepo).save(planning);
    }

    @Test
    void agentNePeutPasSupprimerUnPlanning() {
        agentAuthentication("agent-1");
        assertThrows(AccessDeniedException.class, () -> service.delete("p1"));
        verifyNoInteractions(planningRepo);
    }

    private PlanningTravaux planning(String id, String devisId, String assignedAgentId) {
        Agent agent = new Agent();
        agent.setId(assignedAgentId);
        Devis devis = new Devis();
        devis.setId(devisId);
        devis.setDevisCode("DV-1");
        AffectationDevis affectation = new AffectationDevis();
        affectation.setAgent(agent);
        affectation.setDevis(devis);
        affectation.setActive(true);
        devis.getAffectations().add(affectation);
        Travaux travaux = new Travaux();
        travaux.setLibelle("Travaux BT");
        PlanningTravaux planning = new PlanningTravaux();
        planning.setId(id);
        planning.setDevis(devis);
        planning.setTravaux(travaux);
        planning.setAgent(agent);
        planning.setDateDebut(LocalDateTime.of(2026, 1, 1, 8, 0));
        return planning;
    }

    private void agentAuthentication(String agentId) {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken(agentId, "", "ROLE_AGENT"));
    }
}
