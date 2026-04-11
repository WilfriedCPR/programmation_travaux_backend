package com.gescli.ProgrammationTravaux.mapper;

import com.gescli.ProgrammationTravaux.dto.AgentLiteDTO;
import com.gescli.ProgrammationTravaux.dto.ParticipantDTO;
import com.gescli.ProgrammationTravaux.dto.PlanningTravauxRequestDTO;
import com.gescli.ProgrammationTravaux.dto.PlanningTravauxResponseDTO;
import com.gescli.ProgrammationTravaux.entity.Agent;
import com.gescli.ProgrammationTravaux.entity.Devis;
import com.gescli.ProgrammationTravaux.entity.PlanningTravaux;
import com.gescli.ProgrammationTravaux.entity.Travaux;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class PlanningTravauxMapper {

    public static PlanningTravauxResponseDTO toDTO(PlanningTravaux p) {
        if (p == null) return null;
        PlanningTravauxResponseDTO dto = new PlanningTravauxResponseDTO();
        dto.setId(p.getId());
        dto.setDateDebut(p.getDateDebut() != null ? p.getDateDebut().toString() : null);
        dto.setDateFin(p.getDateFin() != null ? p.getDateFin().toString() : null);
        dto.setDevisId(p.getDevis() != null ? p.getDevis().getId() : null);
        dto.setDevisCode(p.getDevis() != null ? p.getDevis().getDevisCode() : null);
        dto.setTravauxId(p.getTravaux() != null ? p.getTravaux().getId() : null);
        dto.setTravauxLibelle(p.getTravaux() != null ? p.getTravaux().getLibelle() : null);
        dto.setTypeTravauxLibelle(p.getTravaux() != null && p.getTravaux().getTypeTravaux() != null
                ? p.getTravaux().getTypeTravaux().getLibelle() : null);
        dto.setAgentId(p.getAgent() != null ? p.getAgent().getId() : null);
        dto.setAgentNomComplet(p.getAgent() != null
                ? (p.getAgent().getNom() + " " + (p.getAgent().getPrenom() != null ? p.getAgent().getPrenom() : "")).trim()
                : null);
        dto.setCloture(p.isCloture());
        dto.setClotureDate(p.getClotureDate() != null ? p.getClotureDate().toString() : null);
        dto.setIsHt(p.isHt());
        dto.setDemOption(p.getDemOption());

        // Agent de la SONABEL
        Set<Agent> agentParticipants = p.getParticipants();
        if (agentParticipants != null && !agentParticipants.isEmpty()) {
            dto.setParticipants(agentParticipants.stream()
                    .map(a -> new AgentLiteDTO(a.getId(), a.getNom(), a.getPrenom(), a.getCode()))
                    .collect(Collectors.toList()));
        } else {
            dto.setParticipants(Collections.emptyList());
        }

        // Participants externes
        if (p.getParticipantsExternes() != null && !p.getParticipantsExternes().isEmpty()) {
            List<ParticipantDTO> externesList = p.getParticipantsExternes().stream()
                    .map(part -> new ParticipantDTO(
                            part.getId(), part.getNom(), part.getPrenom(),
                            part.getEntreprise(), part.getContact(), part.isExterne()))
                    .collect(Collectors.toList());
            dto.setParticipantsExternesList(externesList);
            dto.setParticipantsExternes(p.getParticipantsExternes().stream()
                    .map(part -> (part.getNom() + " " + (part.getPrenom() != null ? part.getPrenom() : "")).trim())
                    .collect(Collectors.joining(", ")));
        } else {
            dto.setParticipantsExternesList(Collections.emptyList());
            dto.setParticipantsExternes("");
        }

        return dto;
    }

    public static PlanningTravaux toEntity(PlanningTravauxRequestDTO dto, Devis devis, Travaux travaux, Agent agent) {
        PlanningTravaux p = new PlanningTravaux();
        p.setDateDebut(dto.getDateDebut());
        p.setDateFin(dto.getDateFin());
        p.setDevis(devis);
        p.setTravaux(travaux);
        p.setAgent(agent);
        p.setDemOption(dto.getDemOption());
        p.setHt(dto.getDemOption() != null && dto.getDemOption().trim().toUpperCase().endsWith("HT"));
        p.setCloture(dto.getCloture() != null && dto.getCloture());
        return p;
    }
}
