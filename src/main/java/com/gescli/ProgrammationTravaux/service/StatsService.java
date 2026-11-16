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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

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

    // Les statistiques Agent sont calculées après filtrage du périmètre métier.

    private final DevisRepository devisRepo;
    private final PlanningTravauxRepository planningRepo;
    private final AffectationDevisRepository affectationRepo;
    private final AgentRepository agentRepo;
    private final BonSortieMaterielRepository bonSortieRepo;
    private final DemandeMaterielRepository demandeRepo;
    private final ParticipantRepository participantRepo;
    private final TravauxRepository travauxRepo;
    private final MaterielRepository materielRepo;
    private final TypeTravauxRepository typeTravauxRepo;

    @Transactional(readOnly = true)
    public StatsDTO getStats() {
        if (isRestrictedAgent()) return getAgentStats();
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
        long supprimes = devisRepo.countSupprimes();
        long agentsTotal = agentRepo.countByActifTrue();
        long agentsInactifs = agentRepo.countByActifFalse();
        long agentsAffectes = affectationRepo.countDistinctAgentsActive();
        long participantsExternes = participantRepo.countByExterneTrue();

        StatsDTO dto = new StatsDTO();
        dto.setTotalDevisEnCours(enCours);
        dto.setTotalDevisClos(clos);
        dto.setTotalDevisSupprimes(supprimes);
        dto.setTotalDevis(enCours + clos);
        dto.setPlanningsAujourdhui(planningRepo.countByDateRange(startOfDay, endOfDay));
        dto.setPlanningsSemaine(planningRepo.countByDateRange(startOfWeek, endOfWeek));
        dto.setAgentsAffectes(agentsAffectes);
        dto.setAgentsDisponibles(Math.max(0, agentsTotal - agentsAffectes));
        dto.setAgentsInactifs(agentsInactifs);
        dto.setAgentsIndisponibles(planningRepo.countAgentsCurrentlyBusy(now));
        dto.setTravauxEnRetard(planningRepo.countOverdue(now));
        dto.setAgentsTotal(agentsTotal);
        dto.setAgentsCatalogueTotal(agentRepo.count());
        dto.setParticipantsExternesTotal(participantsExternes);
        dto.setTotalTravaux(travauxRepo.count());
        dto.setTotalMateriels(materielRepo.count());
        dto.setTotalTypesTravaux(typeTravauxRepo.count());
        dto.setAgentsTotalAvecExternes(agentsTotal + participantsExternes);
        dto.setAgentsAffectesAujourdhui(affectationRepo.countDistinctAgentsActiveBetween(startOfDay, endOfDay));
        dto.setNouveauxParticipantsAujourdhui(participantRepo.countByExterneTrueAndDateCreationBetween(startOfDay, endOfDay));
        dto.setBonsSortieMois(bonSortieRepo.countByDateSortieBetween(startOfMonth, endOfMonth));
        dto.setBonsSortieAujourdhui(bonSortieRepo.countByDateSortieBetween(startOfDay, endOfDay));
        dto.setBonsSortieATraiter(demandeRepo.countBonsSortieATraiter());
        dto.setDemandesMaterielEnAttente(demandeRepo.countByValideFalseAndDeletedFalse());
        dto.setDemandesMaterielAujourdhui(demandeRepo.countByDeletedFalseAndDateDemandeBetween(startOfDay, endOfDay));
        dto.setDernieresActivites(getActivitesRecentes(devisRepo.findTop5ByOrderByDateCreationDesc(),
                planningRepo.findTop5ByOrderByDateDebutDesc(), bonSortieRepo.findTop5ByOrderByDateSortieDesc()));
        return dto;
    }

    private StatsDTO getAgentStats() {
        String agentId = SecurityContextHolder.getContext().getAuthentication().getName();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfDay = now.toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);
        LocalDateTime startOfWeek = now.toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay();
        LocalDateTime endOfWeek = startOfWeek.plusWeeks(1);
        LocalDateTime startOfMonth = now.toLocalDate().withDayOfMonth(1).atStartOfDay();
        LocalDateTime endOfMonth = startOfMonth.plusMonths(1);

        List<PlanningTravaux> plannings = planningRepo.findVisibleForAgent(agentId);
        List<Devis> devis = devisRepo.findAllById(plannings.stream().map(p -> p.getDevis().getId()).distinct().toList());
        List<BonSortieMateriel> bons = devis.stream().flatMap(d -> bonSortieRepo.findByDemandeMateriel_Devis_Id(d.getId()).stream()).toList();
        var demandes = devis.stream().flatMap(d -> demandeRepo.findByDevisIdAndDeletedFalse(d.getId()).stream()).toList();

        StatsDTO dto = new StatsDTO();
        dto.setTotalDevisEnCours(devis.stream().filter(d -> d.getStatut() == DevisStatut.EN_COURS).count());
        dto.setTotalDevisClos(devis.stream().filter(d -> d.getStatut() == DevisStatut.CLOS).count());
        dto.setTotalDevis(devis.size());
        dto.setTotalDevisSupprimes(0);
        dto.setTotalTravaux(travauxRepo.count());
        dto.setTotalTypesTravaux(typeTravauxRepo.count());
        dto.setPlanningsAujourdhui(plannings.stream().filter(p -> inRange(p.getDateDebut(), startOfDay, endOfDay)).count());
        dto.setPlanningsSemaine(plannings.stream().filter(p -> inRange(p.getDateDebut(), startOfWeek, endOfWeek)).count());
        dto.setTravauxEnRetard(plannings.stream().filter(p -> !p.isCloture() && p.getDateFin() != null && p.getDateFin().isBefore(now)).count());
        boolean active = !plannings.isEmpty();
        boolean busy = plannings.stream().anyMatch(p -> p.getDateDebut() != null && !p.getDateDebut().isAfter(now)
                && (p.getDateFin() == null || !p.getDateFin().isBefore(now)) && !p.isCloture());
        dto.setAgentsTotal(1);
        dto.setAgentsTotalAvecExternes(1);
        dto.setAgentsAffectes(active ? 1 : 0);
        dto.setAgentsDisponibles(busy ? 0 : 1);
        dto.setAgentsIndisponibles(busy ? 1 : 0);
        dto.setAgentsAffectesAujourdhui(plannings.stream().anyMatch(p -> inRange(p.getDateDebut(), startOfDay, endOfDay)) ? 1 : 0);
        dto.setBonsSortieMois(bons.stream().filter(b -> inRange(b.getDateSortie(), startOfMonth, endOfMonth)).count());
        dto.setBonsSortieAujourdhui(bons.stream().filter(b -> inRange(b.getDateSortie(), startOfDay, endOfDay)).count());
        dto.setBonsSortieATraiter(demandes.stream().filter(d -> d.isValide() && bons.stream().noneMatch(b -> b.getDemandeMateriel() != null && b.getDemandeMateriel().getId().equals(d.getId()) && b.getBonInitial() == null)).count());
        dto.setDemandesMaterielEnAttente(demandes.stream().filter(d -> !d.isValide()).count());
        dto.setDemandesMaterielAujourdhui(demandes.stream().filter(d -> inRange(d.getDateDemande(), startOfDay, endOfDay)).count());
        dto.setDernieresActivites(getActivitesRecentes(devis, plannings, bons));
        return dto;
    }

    private boolean isRestrictedAgent() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated() && auth.getAuthorities().stream().anyMatch(a -> "ROLE_AGENT".equals(a.getAuthority()))
                && auth.getAuthorities().stream().noneMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()) || "ROLE_CHEF".equals(a.getAuthority()));
    }

    private boolean inRange(LocalDateTime value, LocalDateTime start, LocalDateTime end) {
        return value != null && !value.isBefore(start) && value.isBefore(end);
    }

    private List<ActiviteDTO> getActivitesRecentes(List<Devis> devisSource, List<PlanningTravaux> planningSource,
            List<BonSortieMateriel> bonsSource) {
        List<ActiviteDTO> activites = new ArrayList<>();

        for (Devis d : devisSource.stream().sorted(Comparator.comparing(Devis::getDateCreation, Comparator.nullsLast(Comparator.reverseOrder()))).limit(5).toList()) {
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

        for (PlanningTravaux p : planningSource.stream().sorted(Comparator.comparing(PlanningTravaux::getDateDebut, Comparator.nullsLast(Comparator.reverseOrder()))).limit(5).toList()) {
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

        for (BonSortieMateriel b : bonsSource.stream().sorted(Comparator.comparing(BonSortieMateriel::getDateSortie, Comparator.nullsLast(Comparator.reverseOrder()))).limit(5).toList()) {
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
