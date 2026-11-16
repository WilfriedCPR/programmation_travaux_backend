package com.gescli.ProgrammationTravaux.service;

import com.gescli.ProgrammationTravaux.dto.DemandeMaterielDTO;
import com.gescli.ProgrammationTravaux.entity.DemandeMateriel;
import com.gescli.ProgrammationTravaux.entity.Devis;
import com.gescli.ProgrammationTravaux.repository.DemandeMaterielRepository;
import com.gescli.ProgrammationTravaux.repository.DevisRepository;
import com.gescli.ProgrammationTravaux.repository.MaterielRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DemandeMaterielServiceTest {

    @Mock DemandeMaterielRepository demandeMaterielRepository;
    @Mock DevisRepository devisRepository;
    @Mock MaterielRepository materielRepository;
    @InjectMocks DemandeMaterielService service;

    @Test
    void updatePreservesOriginalRequestDateWhenDtoDoesNotSendIt() {
        LocalDateTime originalDate = LocalDateTime.of(2026, 9, 17, 10, 30);
        Devis devis = new Devis();
        devis.setId("d1");

        DemandeMateriel existing = new DemandeMateriel();
        existing.setId("m1");
        existing.setDevis(devis);
        existing.setReferenceValue("REF-1");
        existing.setDateDemande(originalDate);
        existing.setLignes(new ArrayList<>());

        DemandeMaterielDTO dto = new DemandeMaterielDTO();
        dto.setDevisId("d1");
        dto.setReferenceValue("REF-1-MOD");
        dto.setDateDemande(null);
        dto.setLignes(new ArrayList<>());

        when(demandeMaterielRepository.existsByDevisIdAndReferenceValueAndIdNotAndDeletedFalse("d1", "REF-1-MOD", "m1"))
                .thenReturn(false);
        when(demandeMaterielRepository.findById("m1")).thenReturn(Optional.of(existing));
        when(devisRepository.findById("d1")).thenReturn(Optional.of(devis));
        when(demandeMaterielRepository.save(any(DemandeMateriel.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DemandeMateriel updated = service.update("m1", dto);

        assertEquals(originalDate, updated.getDateDemande());
    }
}
