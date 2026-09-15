package com.gescli.ProgrammationTravaux.service;

import com.gescli.ProgrammationTravaux.dto.DevisRequestDTO;
import com.gescli.ProgrammationTravaux.dto.DevisResponseDTO;
import com.gescli.ProgrammationTravaux.entity.Client;
import com.gescli.ProgrammationTravaux.entity.DemandeDossier;
import com.gescli.ProgrammationTravaux.entity.Devis;
import com.gescli.ProgrammationTravaux.entity.Document;
import com.gescli.ProgrammationTravaux.entity.DevisStatut;
import com.gescli.ProgrammationTravaux.mapper.DevisMapper;
import com.gescli.ProgrammationTravaux.repository.BonSortieMaterielRepository;
import com.gescli.ProgrammationTravaux.repository.ClientRepository;
import com.gescli.ProgrammationTravaux.repository.DemandeDossierRepository;
import com.gescli.ProgrammationTravaux.repository.DevisRepository;
import com.gescli.ProgrammationTravaux.repository.DocumentRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DevisService {

    private final DevisRepository devisRepository;
    private final DevisMapper devisMapper;
    private final DocumentRepository documentRepository;
    private final BonSortieMaterielRepository bonSortieMaterielRepository;
    private final ClientRepository clientRepository;
    private final DemandeDossierRepository demandeDossierRepository;
    private final ActivityLogService activityLogService;
    private final AffectationService affectationService;

    @Transactional(readOnly = true)
    public Page<DevisResponseDTO> getAllDevis(Pageable pageable, String statut, String q) {
        boolean hasStatut = statut != null && !statut.isBlank();
        boolean hasQuery  = q != null && !q.isBlank();

        DevisStatut st = null;
        if (hasStatut) {
            try {
                st = DevisStatut.valueOf(statut.toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("Statut inconnu : {}", statut);
            }
        }

        if (st != null && hasQuery) {
            return devisRepository.searchByStatut(q.trim(), st, pageable).map(devisMapper::toDto);
        }
        if (st != null) {
            return devisRepository.findAllByStatut(st, pageable).map(devisMapper::toDto);
        }
        if (hasQuery) {
            return devisRepository.search(q.trim(), pageable).map(devisMapper::toDto);
        }
        return devisRepository.findAll(pageable).map(devisMapper::toDto);
    }

    @Transactional(readOnly = true)
    public List<DevisResponseDTO> getAllDevisList(String statut, String q) {
        boolean hasStatut = statut != null && !statut.isBlank();
        boolean hasQuery  = q != null && !q.isBlank();

        DevisStatut st = null;
        if (hasStatut) {
            try {
                st = DevisStatut.valueOf(statut.toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("Statut inconnu : {}", statut);
            }
        }

        List<Devis> results;
        if (st != null && hasQuery) {
            results = devisRepository.searchAllByStatut(q.trim(), st);
        } else if (st != null) {
            if (st == DevisStatut.EN_COURS) {
                results = devisRepository.findAllEnCours();
            } else if (st == DevisStatut.SUPPRIME) {
                results = devisRepository.findAllSupprimes();
            } else {
                results = devisRepository.findAllClos();
            }
        } else if (hasQuery) {
            results = devisRepository.searchAll(q.trim());
        } else {
            results = devisRepository.findTop500ByOrderByDateCreationDesc();
        }
        return results.stream().map(devisMapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public DevisResponseDTO getDevisById(String id) {
        Devis devis = findById(id);
        // Force l'initialisation des relations lazy
        if (devis.getDossier() != null) {
            devis.getDossier().getId(); // init dossier
            if (devis.getDossier().getClient() != null) {
                devis.getDossier().getClient().getId();
            }
        }
        return devisMapper.toDto(devis);
    }

    @Transactional
    public DevisResponseDTO createDevis(DevisRequestDTO dto) {
        if (devisRepository.findByDevisCode(dto.getDevisCode()).isPresent()) {
            throw new IllegalStateException("Un devis avec le code " + dto.getDevisCode() + " existe déjà.");
        }
        Devis devis = new Devis();
        devis.setDevisCode(dto.getDevisCode());
        applyDateCreation(devis, dto.getDateCreation());
        applyStatut(devis, dto.getStatut());
        applyDocument(devis, dto.getDocumentId());
        Devis saved = devisRepository.save(devis);
        activityLogService.log("DEVIS_CREATION", "Création du devis " + saved.getDevisCode(), saved.getDevisCode(), saved.getId(), null);
        return devisMapper.toDto(saved);
    }

    @Transactional
    public DevisResponseDTO updateDevis(String id, DevisRequestDTO dto) {
        Devis devis = findById(id);
        if (dto.getDevisCode() != null && !dto.getDevisCode().equals(devis.getDevisCode())
                && devisRepository.findByDevisCode(dto.getDevisCode()).isPresent()) {
            throw new IllegalStateException("Un autre devis avec le code " + dto.getDevisCode() + " existe déjà.");
        }
        if (dto.getDevisCode() != null && !dto.getDevisCode().isBlank()) {
            devis.setDevisCode(dto.getDevisCode());
        }
        applyDateCreation(devis, dto.getDateCreation());
        applyDocument(devis, dto.getDocumentId());
        if (dto.getStatut() != null && !dto.getStatut().isBlank()) {
            log.warn("Statut de devis ignoré lors de la mise à jour : {}", dto.getStatut());
        }
        if (dto.getClientCode() != null && !dto.getClientCode().isBlank()) {
            log.warn("Client de devis ignoré lors de la mise à jour : {}", dto.getClientCode());
        }
        applyDossierUpdate(devis, null, dto.getDemOption());
        Devis saved = devisRepository.save(devis);
        activityLogService.log("DEVIS_MODIFICATION", "Modification du devis " + saved.getDevisCode(), saved.getDevisCode(), saved.getId(), null);
        return devisMapper.toDto(saved);
    }

    @Transactional
    public void deleteDevis(String id) {
        Devis devis = findById(id);
        devis.setStatut(DevisStatut.SUPPRIME);
        devis.setDateSuppression(LocalDateTime.now());
        devisRepository.save(devis);
        affectationService.terminerAffectationsDuDevis(id, "devis supprimé");
        activityLogService.log("DEVIS_SUPPRESSION", "Suppression logique du devis " + devis.getDevisCode(), devis.getDevisCode(), devis.getId(), null);
    }

    @Transactional
    public void deleteDevisHard(String id) {
        Devis devis = findById(id);
        if (devis.getStatut() != DevisStatut.SUPPRIME && devis.getStatut() != DevisStatut.CLOS) {
            throw new IllegalStateException("Suppression définitive autorisée uniquement pour les devis SUPPRIMES ou CLOS.");
        }
        activityLogService.log("DEVIS_SUPPRESSION_DEFINITIVE", "Suppression définitive du devis " + devis.getDevisCode(), devis.getDevisCode(), devis.getId(), null);
        devisRepository.deleteById(id);
    }

    @Transactional
    public void restoreDevis(String id) {
        Devis devis = findById(id);
        devis.setStatut(DevisStatut.EN_COURS);
        devisRepository.save(devis);
        activityLogService.log("DEVIS_RESTAURATION", "Restauration du devis " + devis.getDevisCode(), devis.getDevisCode(), devis.getId(), null);
    }

    @Transactional(readOnly = true)
    public boolean hasBonSortie(String devisId) {
        return bonSortieMaterielRepository.countByDemandeMateriel_Devis_Id(devisId) > 0;
    }

    @Transactional
    public DevisResponseDTO closeDevis(String devisId, String observation) {
        Devis devis = findById(devisId);
        if (bonSortieMaterielRepository.countByDemandeMateriel_Devis_Id(devisId) == 0) {
            throw new IllegalStateException("Veuillez générer au moins un bon de sortie matériel avant de clôturer.");
        }
        devis.setStatut(DevisStatut.CLOS);
        devis.setDateSuppression(LocalDateTime.now());
        Devis saved = devisRepository.save(devis);
        affectationService.terminerAffectationsDuDevis(devisId, "devis clôturé");
        activityLogService.log("DEVIS_CLOTURE", "Clôture du devis " + saved.getDevisCode(), saved.getDevisCode(), saved.getId(), null);
        return devisMapper.toDto(saved);
    }

    private Devis findById(String id) {
        return devisRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Devis non trouvé : " + id));
    }

    private void applyDateCreation(Devis devis, String dateStr) {
        if (dateStr != null && !dateStr.isBlank()) {
            try {
                devis.setDateCreation(LocalDate.parse(dateStr).atStartOfDay());
            } catch (Exception ignored) {
                log.warn("Format de date invalide : {}", dateStr);
            }
        }
    }

    private void applyStatut(Devis devis, String statutStr) {
        if (statutStr != null && !statutStr.isBlank()) {
            try {
                devis.setStatut(DevisStatut.valueOf(statutStr.toUpperCase()));
            } catch (IllegalArgumentException e) {
                log.warn("Statut invalide ignoré : {}", statutStr);
            }
        }
    }

    private void applyDocument(Devis devis, String documentId) {
        if (documentId != null) {
            Document doc = documentRepository.findById(documentId)
                    .orElseThrow(() -> new EntityNotFoundException("Document non trouvé : " + documentId));
            devis.setDocument(doc);
        } else {
            devis.setDocument(null);
        }
    }

    private void applyDossierUpdate(Devis devis, String clientCode, String demOption) {
        if ((clientCode == null || clientCode.isBlank()) && (demOption == null || demOption.isBlank())) return;
        DemandeDossier dossier = devis.getDossier();
        if (dossier == null) {
            dossier = new DemandeDossier();
            dossier.setId(java.util.UUID.randomUUID().toString());
            dossier = demandeDossierRepository.save(dossier);
            devis.setDossier(dossier);
        }
        if (demOption != null && !demOption.isBlank()) {
            dossier.setDemOption(demOption);
        }
        if (clientCode != null && !clientCode.isBlank()) {
            Client client = clientRepository.findByCliCode(clientCode).orElse(null);
            if (client != null) dossier.setClient(client);
        }
        demandeDossierRepository.save(dossier);
    }
}