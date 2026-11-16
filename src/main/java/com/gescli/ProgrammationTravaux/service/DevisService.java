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
import com.gescli.ProgrammationTravaux.repository.PlanningTravauxRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.data.domain.PageImpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@Service
@RequiredArgsConstructor
public class DevisService {

    private final DevisRepository devisRepository;
    private final DevisMapper devisMapper;
    private final DocumentRepository documentRepository;
    private final PlanningTravauxRepository planningRepository;
    private final FileStorageService fileStorageService;
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

        Page<Devis> page;
        if (st != null && hasQuery) {
            page = devisRepository.searchByStatut(q.trim(), st, pageable);
        } else if (st != null) {
            page = devisRepository.findAllByStatut(st, pageable);
        } else if (hasQuery) {
            page = devisRepository.search(q.trim(), pageable);
        } else {
            page = devisRepository.findAll(pageable);
        }
        if (isRestrictedAgent()) {
            List<Devis> visible = page.getContent().stream().filter(this::isVisibleToCurrentAgent).toList();
            page = new PageImpl<>(visible, pageable, visible.size());
        }
        return page.map(devisMapper::toDto);
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
        return results.stream().filter(d -> !isRestrictedAgent() || isVisibleToCurrentAgent(d))
                .map(devisMapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public DevisResponseDTO getDevisById(String id) {
        Devis devis = findById(id);
        ensureVisibleToCurrentAgent(devis);
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
    public DevisResponseDTO closeDevis(String devisId, MultipartFile finalPv, String observation) throws java.io.IOException {
        Devis devis = findById(devisId);
        if (devis.getStatut() == DevisStatut.CLOS) return devisMapper.toDto(devis);
        if (devis.getStatut() != DevisStatut.EN_COURS) throw new IllegalStateException("Seul un devis EN_COURS peut être clôturé.");
        var plannings = planningRepository.findByDevisIdAndDeletedFalse(devisId);
        if (plannings.isEmpty() || plannings.stream().anyMatch(p -> p.getDateFin() == null && !p.isCloture())) {
            throw new IllegalStateException("Tous les plannings actifs du devis doivent être terminés avant la clôture.");
        }
        if (bonSortieMaterielRepository.countByDemandeMateriel_Devis_Id(devisId) == 0) {
            throw new IllegalStateException("Veuillez générer au moins un bon de sortie matériel avant de clôturer.");
        }
        boolean hta = plannings.stream().anyMatch(p -> p.isHt() || (p.getDemOption() != null && p.getDemOption().trim().toUpperCase().endsWith("HT")));
        com.gescli.ProgrammationTravaux.entity.PvKind requiredKind = hta
                ? com.gescli.ProgrammationTravaux.entity.PvKind.RECEPTION_DEFINITIVE
                : com.gescli.ProgrammationTravaux.entity.PvKind.FINAL_BT;
        boolean existingFinalPv = plannings.stream().anyMatch(p -> documentRepository.existsByPlanning_IdAndTypeDocumentAndPvKind(p.getId(), com.gescli.ProgrammationTravaux.entity.TypeDocument.PV, requiredKind));
        if (finalPv == null && !existingFinalPv) {
            throw new IllegalStateException("Le PV final requis (" + requiredKind + ") doit être fourni avant la clôture.");
        }
        String storedPath = null;
        try {
            if (finalPv != null && !finalPv.isEmpty()) {
                validateFinalPv(finalPv);
                storedPath = fileStorageService.store(finalPv, "pv");
                var doc = new Document();
                doc.setPlanning(plannings.get(plannings.size() - 1));
                doc.setTypeDocument(com.gescli.ProgrammationTravaux.entity.TypeDocument.PV);
                doc.setPvKind(requiredKind);
                doc.setFileName(finalPv.getOriginalFilename());
                doc.setFilePath(storedPath);
                doc.setMimeType(finalPv.getContentType());
                doc.setObservation(observation);
                documentRepository.save(doc);
            }
            devis.setStatut(DevisStatut.CLOS);
            Devis saved = devisRepository.save(devis);
            affectationService.terminerAffectationsDuDevis(devisId, "devis clôturé");
            activityLogService.log("DEVIS_CLOTURE", "Clôture du devis " + saved.getDevisCode(), saved.getDevisCode(), saved.getId(), null);
            return devisMapper.toDto(saved);
        } catch (RuntimeException | java.io.IOException ex) {
            if (storedPath != null) try { Files.deleteIfExists(Path.of(storedPath)); } catch (java.io.IOException cleanup) { log.warn("Nettoyage du PV échoué", cleanup); }
            throw ex;
        }
    }

    private void validateFinalPv(MultipartFile file) {
        if (file.getSize() <= 0 || file.getSize() > 10 * 1024 * 1024) throw new IllegalArgumentException("Le PV doit faire entre 1 octet et 10 Mo.");
        if (file.getContentType() == null || !file.getContentType().equalsIgnoreCase("application/pdf")) throw new IllegalArgumentException("Le PV final doit être un fichier PDF.");
    }

    private Devis findById(String id) {
        return devisRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Devis non trouvé : " + id));
    }

    private boolean isRestrictedAgent() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return false;
        boolean privileged = auth.getAuthorities().stream().anyMatch(a ->
                "ROLE_ADMIN".equals(a.getAuthority()) || "ROLE_CHEF".equals(a.getAuthority()));
        boolean agent = auth.getAuthorities().stream().anyMatch(a -> "ROLE_AGENT".equals(a.getAuthority()));
        return agent && !privileged;
    }

    private boolean isVisibleToCurrentAgent(Devis devis) {
        if (!isRestrictedAgent()) return true;
        String agentId = SecurityContextHolder.getContext().getAuthentication().getName();
        return devis.getAffectations().stream().anyMatch(a -> a.isActive() && a.getAgent() != null && agentId.equals(a.getAgent().getId()))
                || devis.getProgrammations().stream().anyMatch(p -> (p.getAgent() != null && agentId.equals(p.getAgent().getId()))
                || p.getParticipants().stream().anyMatch(a -> agentId.equals(a.getId())));
    }

    private void ensureVisibleToCurrentAgent(Devis devis) {
        if (!isVisibleToCurrentAgent(devis)) throw new AccessDeniedException("Accès au devis non autorisé");
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
