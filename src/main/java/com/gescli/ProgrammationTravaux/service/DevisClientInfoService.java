package com.gescli.ProgrammationTravaux.service;

import com.gescli.ProgrammationTravaux.dto.DevisClientInfoDTO;
import com.gescli.ProgrammationTravaux.entity.Client;
import com.gescli.ProgrammationTravaux.entity.DemandeDossier;
import com.gescli.ProgrammationTravaux.entity.Devis;
import com.gescli.ProgrammationTravaux.repository.DevisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DevisClientInfoService {

    private final DevisRepository devisRepository;

    public Optional<DevisClientInfoDTO> getByDevisId(String devisId) {
        return devisRepository.findById(devisId).map(this::toClientInfoDTO);
    }

    public Optional<DevisClientInfoDTO> getByDevisCode(String devisCode) {
        return devisRepository.findByDevisCode(devisCode).map(this::toClientInfoDTO);
    }

    private DevisClientInfoDTO toClientInfoDTO(Devis devis) {
        DevisClientInfoDTO dto = new DevisClientInfoDTO();
        dto.setFound(true);
        DemandeDossier dossier = devis.getDossier();
        if (dossier != null) {
            dto.setDemOption(dossier.getDemOption());
            Client client = dossier.getClient();
            if (client != null) {
                dto.setCliCode(client.getCliCode());
                dto.setCliNom(client.getCliNom());
                dto.setCliPrenom(client.getCliPrenom());
                dto.setCliRaisonSocial(client.getCliRaisonSocial());
                String displayName = (client.getCliRaisonSocial() != null && !client.getCliRaisonSocial().isBlank())
                        ? client.getCliRaisonSocial()
                        : ((client.getCliNom() != null ? client.getCliNom() : "")
                                + (client.getCliPrenom() != null ? " " + client.getCliPrenom() : "")).trim();
                dto.setDisplayName(displayName.isBlank() ? client.getCliCode() : displayName);
            }
        }
        return dto;
    }
}
