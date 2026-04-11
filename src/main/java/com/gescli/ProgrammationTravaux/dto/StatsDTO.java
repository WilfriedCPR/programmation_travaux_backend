package com.gescli.ProgrammationTravaux.dto;

import lombok.Data;
import java.util.List;

@Data
public class StatsDTO {
    private long totalDevisEnCours;
    private long totalDevisClos;
    private long totalDevis;
    private long planningsAujourdhui;
    private long planningsSemaine;
    private long agentsAffectes;
    private long agentsTotal;
    private long participantsExternesTotal;
    private long agentsTotalAvecExternes;
    private long agentsAffectesAujourdhui;
    private long nouveauxParticipantsAujourdhui;
    private long bonsSortieMois;
    private long bonsSortieAujourdhui;
    private long demandesMaterielEnAttente;
    private long demandesMaterielAujourdhui;
    private List<ActiviteDTO> dernieresActivites;
}
