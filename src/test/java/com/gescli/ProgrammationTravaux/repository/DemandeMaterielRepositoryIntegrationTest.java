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
class DemandeMaterielRepositoryIntegrationTest {
    @Autowired DevisRepository devisRepository;
    @Autowired DemandeMaterielRepository demandeRepository;
    @Autowired BonSortieMaterielRepository bonRepository;

    @Test
    void validatedRequestWithoutInitialBonMustBeCountedAsBonSortieToProcess() {
        Devis devis = new Devis();
        devis.setId("d-bon"); devis.setDevisCode("DEV-BON"); devis.setDateCreation(LocalDateTime.now()); devis.setStatut(DevisStatut.EN_COURS);
        devisRepository.saveAndFlush(devis);

        DemandeMateriel demande = new DemandeMateriel();
        demande.setDevis(devis); demande.setValide(true); demande.setDeleted(false); demande.setDateDemande(LocalDateTime.now());
        demande = demandeRepository.saveAndFlush(demande);
        assertThat(demandeRepository.countBonsSortieATraiter()).isEqualTo(1);

        BonSortieMateriel bon = new BonSortieMateriel();
        bon.setNumeroBon("BS-001"); bon.setNumeroDevis("DEV-BON"); bon.setDateSortie(LocalDateTime.now()); bon.setDemandeMateriel(demande);
        bonRepository.saveAndFlush(bon);
        assertThat(demandeRepository.countBonsSortieATraiter()).isZero();
    }
}
