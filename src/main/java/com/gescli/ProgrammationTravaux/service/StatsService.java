package com.gescli.ProgrammationTravaux.service;

import com.gescli.ProgrammationTravaux.dto.ActiviteDTO;
import com.gescli.ProgrammationTravaux.dto.StatsDTO;
import com.gescli.ProgrammationTravaux.entity.BonSortieMateriel;
import com.gescli.ProgrammationTravaux.entity.Devis;
import com.gescli.ProgrammationTravaux.entity.DevisStatut;
import com.gescli.ProgrammationTravaux.entity.PlanningTravaux;
import com.gescli.ProgrammationTravaux.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatsService {

    private final DevisRepository devisRepo;
    private final PlanningTravauxRepository planningRepo;
    private final AffectationDevisRepository affectationRepo;
    private final AgentRepository agentRepo;
    private final BonSortieMaterielRepository bonSortieRepo;
    private final DemandeMaterielRepository demandeRepo;
    private final ParticipantRepository participantRepo;

    @Transactional(readOnly = true)
    public StatsDTO getStats() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfDay = now.toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);
        LocalDateTime startOfWeek = now.toLocalDate()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay();
        LocalDateTime endOfWeek = startOfWeek.plusWeeks(1);
        LocalDateTime startOfMonth = now.toLocalDate().withDayOfMonth(1).atStartOfDay();
        LocalDateTime endOfMonth = startOfMonth.plusMonths(1);

        long enCours = devisRepo.countEnCours();
        long clos = devisRepo.countClos();
        long agentsTotal = agentRepo.count();
        long participantsExternes = participantRepo.countByExterneTrue();

        StatsDTO dto = new StatsDTO();
        dto.setTotalDevisEnCours(enCours);
        dto.setTotalDevisClos(clos);
        dto.setTotalDevis(enCours + clos);
        dto.setPlanningsAujourdhui(planningRepo.countByDateRange(startOfDay, endOfDay));
        dto.setPlanningsSemaine(planningRepo.countByDateRange(startOfWeek, endOfWeek));
        dto.setAgentsAffectes(affectationRepo.countByActiveTrue());
        dto.setAgentsTotal(agentsTotal);
        dto.setParticipantsExternesTotal(participantsExternes);
        dto.setAgentsTotalAvecExternes(agentsTotal + participantsExternes);
        dto.setAgentsAffectesAujourdhui(affectationRepo.countByActiveTrueAndDateAffectationBetween(startOfDay, endOfDay));
        dto.setNouveauxParticipantsAujourdhui(participantRepo.countByExterneTrueAndDateCreationBetween(startOfDay, endOfDay));
        dto.setBonsSortieMois(bonSortieRepo.countByDateSortieBetween(startOfMonth, endOfMonth));
        dto.setBonsSortieAujourdhui(bonSortieRepo.countByDateSortieBetween(startOfDay, endOfDay));
        dto.setDemandesMaterielEnAttente(demandeRepo.countByValideFalseAndDeletedFalse());
        dto.setDemandesMaterielAujourdhui(demandeRepo.countByDeletedFalseAndDateDemandeBetween(startOfDay, endOfDay));
        dto.setDernieresActivites(getActivitesRecentes());
        return dto;
    }

    private List<ActiviteDTO> getActivitesRecentes() {
        List<ActiviteDTO> activites = new ArrayList<>();

        for (Devis d : devisRepo.findTop5ByOrderByDateCreationDesc()) {
            ActiviteDTO a = new ActiviteDTO();
            a.setId(d.getId());
            a.setType("DEVIS");
            a.setDescription("Devis N° " + d.getDevisCode());
            a.setDate(d.getDateCreation());
            a.setActeur(null);
            a.setReference(d.getDevisCode());
            a.setDevisId(d.getId());
            activites.add(a);
        }

        for (PlanningTravaux p : planningRepo.findTop5ByOrderByDateDebutDesc()) {
            ActiviteDTO a = new ActiviteDTO();
            a.setId(p.getId());
            a.setType("PLANNING");
            a.setDescription("Planning " + p.getDevis().getDevisCode() + " — " + p.getTravaux().getLibelle());
            a.setDate(p.getDateDebut());
            a.setActeur(null);
            a.setReference(p.getDevis().getDevisCode());
            a.setDevisId(p.getDevis().getId());
            activites.add(a);
        }

        for (BonSortieMateriel b : bonSortieRepo.findTop5ByOrderByDateSortieDesc()) {
            ActiviteDTO a = new ActiviteDTO();
            a.setId(b.getId());
            a.setType("BON_SORTIE");
            a.setDescription("Bon de sortie N° " + b.getNumeroBon());
            a.setDate(b.getDateSortie());
            a.setActeur(null);
            a.setReference(b.getNumeroBon());
            if (b.getDemandeMateriel() != null && b.getDemandeMateriel().getDevis() != null) {
                a.setDevisId(b.getDemandeMateriel().getDevis().getId());
            }
            activites.add(a);
        }

        return activites.stream()
                .sorted(Comparator.comparing(ActiviteDTO::getDate).reversed())
                .limit(10)
                .collect(Collectors.toList());
    }
}
