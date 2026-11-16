package com.gescli.ProgrammationTravaux.service;

import com.gescli.ProgrammationTravaux.entity.Devis;
import com.gescli.ProgrammationTravaux.entity.DevisStatut;
import com.gescli.ProgrammationTravaux.entity.PlanningTravaux;
import com.gescli.ProgrammationTravaux.entity.PvKind;
import com.gescli.ProgrammationTravaux.entity.Agent;
import com.gescli.ProgrammationTravaux.entity.AffectationDevis;
import com.gescli.ProgrammationTravaux.mapper.DevisMapper;
import com.gescli.ProgrammationTravaux.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DevisServiceTest {

    @Mock DevisRepository devisRepository;
    @Mock DevisMapper devisMapper;
    @Mock DocumentRepository documentRepository;
    @Mock BonSortieMaterielRepository bonRepository;
    @Mock ClientRepository clientRepository;
    @Mock DemandeDossierRepository dossierRepository;
    @Mock PlanningTravauxRepository planningRepository;
    @Mock ActivityLogService activityLogService;
    @Mock AffectationService affectationService;
    @Mock FileStorageService fileStorageService;
    @InjectMocks DevisService service;

    @Test
    void refuseLaClotureSiUnPlanningNestPasTermine() {
        Devis devis = devis(DevisStatut.EN_COURS);
        PlanningTravaux planning = new PlanningTravaux();
        planning.setCloture(false);
        when(devisRepository.findById("d1")).thenReturn(Optional.of(devis));
        when(planningRepository.findByDevisIdAndDeletedFalse("d1")).thenReturn(List.of(planning));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.closeDevis("d1", null, null));

        assertTrue(ex.getMessage().contains("plannings"));
        verifyNoInteractions(bonRepository, affectationService);
    }

    @Test
    void accepteLaClotureAvecUnPlanningDateFinMaisPasEncoreCloture() throws Exception {
        Devis devis = devis(DevisStatut.EN_COURS);
        PlanningTravaux planning = planning(false, false);
        planning.setDateFin(java.time.LocalDateTime.now().minusHours(1));
        when(devisRepository.findById("d1")).thenReturn(Optional.of(devis));
        when(planningRepository.findByDevisIdAndDeletedFalse("d1")).thenReturn(List.of(planning));
        when(bonRepository.countByDemandeMateriel_Devis_Id("d1")).thenReturn(1L);
        when(fileStorageService.store(any(), eq("pv"))).thenReturn("target-pv.pdf");
        when(devisRepository.save(devis)).thenReturn(devis);
        when(devisMapper.toDto(devis)).thenReturn(new com.gescli.ProgrammationTravaux.dto.DevisResponseDTO());

        var file = new MockMultipartFile("file", "pv.pdf", "application/pdf", "pdf".getBytes());
        service.closeDevis("d1", file, null);

        assertEquals(DevisStatut.CLOS, devis.getStatut());
        verify(devisRepository).save(devis);
    }

    @Test
    void refuseUnPvNonPdf() throws Exception {
        Devis devis = devis(DevisStatut.EN_COURS);
        PlanningTravaux planning = planning(false, true);
        when(devisRepository.findById("d1")).thenReturn(Optional.of(devis));
        when(planningRepository.findByDevisIdAndDeletedFalse("d1")).thenReturn(List.of(planning));
        when(bonRepository.countByDemandeMateriel_Devis_Id("d1")).thenReturn(1L);

        MockMultipartFile file = new MockMultipartFile("file", "pv.txt", "text/plain", "x".getBytes());
        assertThrows(IllegalArgumentException.class, () -> service.closeDevis("d1", file, null));
        verify(devisRepository, never()).save(any());
    }

    @Test
    void clotureUnDevisBtEtEstIdempotente() throws Exception {
        Devis devis = devis(DevisStatut.EN_COURS);
        PlanningTravaux planning = planning(false, true);
        Path stored = Files.createTempFile("pv-test-", ".pdf");
        when(devisRepository.findById("d1")).thenReturn(Optional.of(devis));
        when(planningRepository.findByDevisIdAndDeletedFalse("d1")).thenReturn(List.of(planning));
        when(bonRepository.countByDemandeMateriel_Devis_Id("d1")).thenReturn(1L);
        when(fileStorageService.store(any(), eq("pv"))).thenReturn(stored.toString());
        when(devisRepository.save(devis)).thenReturn(devis);
        when(devisMapper.toDto(devis)).thenReturn(new com.gescli.ProgrammationTravaux.dto.DevisResponseDTO());

        var file = new MockMultipartFile("file", "pv.pdf", "application/pdf", "pdf".getBytes());
        service.closeDevis("d1", file, "ok");
        assertEquals(DevisStatut.CLOS, devis.getStatut());

        service.closeDevis("d1", null, null);
        verify(devisRepository, times(1)).save(devis);
        Files.deleteIfExists(stored);
    }

    @Test
    void nettoieLeFichierSiLaClotureEchoue() throws Exception {
        Devis devis = devis(DevisStatut.EN_COURS);
        PlanningTravaux planning = planning(false, true);
        Path stored = Files.createTempFile("pv-rollback-", ".pdf");
        when(devisRepository.findById("d1")).thenReturn(Optional.of(devis));
        when(planningRepository.findByDevisIdAndDeletedFalse("d1")).thenReturn(List.of(planning));
        when(bonRepository.countByDemandeMateriel_Devis_Id("d1")).thenReturn(1L);
        when(fileStorageService.store(any(), eq("pv"))).thenReturn(stored.toString());
        doThrow(new IllegalStateException("échec affectation")).when(affectationService).terminerAffectationsDuDevis(eq("d1"), anyString());

        var file = new MockMultipartFile("file", "pv.pdf", "application/pdf", "pdf".getBytes());
        assertThrows(IllegalStateException.class, () -> service.closeDevis("d1", file, null));
        assertFalse(Files.exists(stored));
        verify(devisRepository).save(devis);
        // Le rollback de l'état JPA est couvert par le test d'intégration à exécuter avec une base isolée.
    }

    @Test
    void agentNePeutPasConsulterUnDevisHorsDeSonPerimetre() {
        try {
            Devis devis = devis(DevisStatut.EN_COURS);
            Agent autre = new Agent();
            autre.setId("agent-autre");
            AffectationDevis affectation = new AffectationDevis();
            affectation.setAgent(autre);
            affectation.setDevis(devis);
            affectation.setActive(true);
            devis.getAffectations().add(affectation);
            when(devisRepository.findById("d1")).thenReturn(Optional.of(devis));
            SecurityContextHolder.getContext().setAuthentication(
                    new TestingAuthenticationToken("agent-courant", "", "ROLE_AGENT"));

            assertThrows(org.springframework.security.access.AccessDeniedException.class,
                    () -> service.getDevisById("d1"));
            verify(devisMapper, never()).toDto(any());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private Devis devis(DevisStatut statut) {
        Devis d = new Devis();
        d.setId("d1"); d.setDevisCode("DV-1"); d.setStatut(statut);
        return d;
    }

    private PlanningTravaux planning(boolean ht, boolean closed) {
        PlanningTravaux p = new PlanningTravaux();
        p.setId("p1"); p.setHt(ht); p.setCloture(closed);
        return p;
    }
}
