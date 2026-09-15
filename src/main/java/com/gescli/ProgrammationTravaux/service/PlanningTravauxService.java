package com.gescli.ProgrammationTravaux.service;

import com.gescli.ProgrammationTravaux.dto.PlanningTravauxRequestDTO;
import com.gescli.ProgrammationTravaux.dto.PlanningTravauxResponseDTO;
import com.gescli.ProgrammationTravaux.dto.PvDocumentDTO;
import com.gescli.ProgrammationTravaux.entity.*;
import com.gescli.ProgrammationTravaux.mapper.PlanningTravauxMapper;
import com.gescli.ProgrammationTravaux.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlanningTravauxService {

    private final PlanningTravauxRepository planningRepo;
    private final DevisRepository devisRepo;
    private final TravauxRepository travauxRepo;
    private final TypeTravauxRepository typeTravauxRepo;
    private final AgentRepository agentRepo;
    private final ParticipantRepository participantRepo;
    private final TravauxContrainteRepository contrainteRepo;
    private final MaterielRepository materielRepository;
    private final DocumentRepository documentRepository;
    private final FileStorageService fileStorageService;
    private final ActivityLogService activityLogService;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public List<PlanningTravauxResponseDTO> getAll() {
        return planningRepo.findAll().stream().map(PlanningTravauxMapper::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public List<PlanningTravauxResponseDTO> getByDevis(String devisId) {
        return planningRepo.findByDevisIdAndDeletedFalse(devisId).stream().map(PlanningTravauxMapper::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public List<PlanningTravauxResponseDTO> getDeletedByDevis(String devisId) {
        return planningRepo.findByDevisIdAndDeletedTrue(devisId).stream().map(PlanningTravauxMapper::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public List<PlanningTravauxResponseDTO> getClosedByDevis(String devisId) {
        return planningRepo.findByDevisIdAndClotureTrue(devisId).stream().map(PlanningTravauxMapper::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public PlanningTravauxResponseDTO getById(String id) {
        return PlanningTravauxMapper.toDTO(findById(id));
    }

    @Transactional
    public PlanningTravauxResponseDTO create(PlanningTravauxRequestDTO dto) {
        Devis devis = devisRepo.findById(dto.getDevisId())
                .orElseThrow(() -> new EntityNotFoundException("Devis introuvable : " + dto.getDevisId()));
        Travaux travaux = resolveTravaux(dto);
        validateDevisTravauxType(dto, travaux);
        Agent agent = dto.getAgentId() != null ? agentRepo.findById(dto.getAgentId()).orElse(null) : null;

        PlanningTravaux planning = PlanningTravauxMapper.toEntity(dto, devis, travaux, agent);
        applyParticipants(planning, dto);
        validateScheduleConflicts(planning, null);
        PlanningTravaux saved = planningRepo.save(planning);
        activityLogService.log("PLANNING_CREATION", "Création du planning " + devis.getDevisCode() + " — " + travaux.getLibelle(), devis.getDevisCode(), devis.getId(), agent != null ? agent.getId() : null);
        notifyPlanningMembers(saved, "Nouveau planning", "Un planning a été programmé pour le devis " + devis.getDevisCode() + ".");
        return PlanningTravauxMapper.toDTO(saved);
    }

    @Transactional
    public PlanningTravauxResponseDTO update(String id, PlanningTravauxRequestDTO dto) {
        PlanningTravaux planning = findById(id);
        Devis devis = devisRepo.findById(dto.getDevisId())
                .orElseThrow(() -> new EntityNotFoundException("Devis introuvable : " + dto.getDevisId()));
        Travaux travaux = resolveTravaux(dto);
        validateDevisTravauxType(dto, travaux);
        Agent agent = dto.getAgentId() != null ? agentRepo.findById(dto.getAgentId()).orElse(null) : null;

        if (dto.getDateFin() != null) {
            long activeConstraints = contrainteRepo.countByPlanning_IdAndActiveTrue(id);
            if (activeConstraints > 0) {
                throw new IllegalStateException("Ce planning contient " + activeConstraints
                        + " contrainte(s) active(s). Levez-les avant de le terminer.");
            }
        }

        planning.setDateDebut(dto.getDateDebut());
        planning.setDateFin(dto.getDateFin());
        planning.setDevis(devis);
        planning.setTravaux(travaux);
        planning.setAgent(agent);
        planning.setDemOption(dto.getDemOption());
        planning.setHt(isHt(dto.getDemOption()));
        applyParticipants(planning, dto);
        validateScheduleConflicts(planning, id);
        PlanningTravaux saved = planningRepo.save(planning);
        activityLogService.log("PLANNING_MODIFICATION", "Modification du planning " + devis.getDevisCode() + " — " + travaux.getLibelle(), devis.getDevisCode(), devis.getId(), agent != null ? agent.getId() : null);
        notifyPlanningMembers(saved, "Planning modifié", "Le planning du devis " + devis.getDevisCode() + " a été modifié.");
        return PlanningTravauxMapper.toDTO(saved);
    }

    @Transactional
    public void delete(String id) {
        PlanningTravaux planning = findById(id);
        planning.setDeleted(true);
        planningRepo.save(planning);
        activityLogService.log("PLANNING_SUPPRESSION", "Suppression logique du planning " + planning.getDevis().getDevisCode() + " — " + planning.getTravaux().getLibelle(), planning.getDevis().getDevisCode(), planning.getDevis().getId(), planning.getAgent() != null ? planning.getAgent().getId() : null);
        notifyPlanningMembers(planning, "Planning annulé", "Un planning du devis " + planning.getDevis().getDevisCode() + " a été annulé.");
    }

    @Transactional
    public void hardDelete(String id) {
        PlanningTravaux planning = findById(id);
        if (!planning.isDeleted()) {
            throw new IllegalStateException("Le planning doit être supprimé (soft-delete) avant la suppression définitive.");
        }
        planningRepo.delete(planning);
    }

    @Transactional
    public PlanningTravaux closePlanning(String id) {
        PlanningTravaux planning = findById(id);
        long active = contrainteRepo.countByPlanning_IdAndActiveTrue(id);
        if (active > 0) {
            throw new IllegalStateException("Ce planning contient " + active + " contrainte(s) active(s).");
        }
        planning.setCloture(true);
        planning.setClotureDate(LocalDateTime.now());
        PlanningTravaux saved = planningRepo.save(planning);
        activityLogService.log("PLANNING_CLOTURE", "Clôture du planning " + planning.getDevis().getDevisCode() + " — " + planning.getTravaux().getLibelle(), planning.getDevis().getDevisCode(), planning.getDevis().getId(), planning.getAgent() != null ? planning.getAgent().getId() : null);
        notifyPlanningMembers(saved, "Planning clôturé", "Le planning du devis " + planning.getDevis().getDevisCode() + " a été clôturé.");
        return saved;
    }

    public PlanningTravaux findById(String id) {
        return planningRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("PlanningTravaux introuvable : " + id));
    }

    private Travaux resolveTravaux(PlanningTravauxRequestDTO dto) {
        if (dto.getTravauxId() != null && !dto.getTravauxId().isBlank()) {
            return travauxRepo.findById(dto.getTravauxId())
                    .orElseThrow(() -> new EntityNotFoundException("Travaux introuvable : " + dto.getTravauxId()));
        }
        if (dto.getTravauxName() != null && !dto.getTravauxName().isBlank()) {
            String typeLabel = isHt(dto.getDemOption()) ? "HT" : "BT";
            TypeTravaux type = typeTravauxRepo.findByLibelle(typeLabel)
                    .orElseGet(() -> {
                        TypeTravaux nt = new TypeTravaux();
                        nt.setLibelle(typeLabel);
                        return typeTravauxRepo.save(nt);
                    });
            Travaux t = new Travaux();
            t.setLibelle(dto.getTravauxName());
            t.setTypeTravaux(type);
            return travauxRepo.save(t);
        }
        throw new IllegalArgumentException("Fournir un travauxId existant ou un travauxName.");
    }

    private void validateDevisTravauxType(PlanningTravauxRequestDTO dto, Travaux travaux) {
        if (!isHt(dto.getDemOption())) {
            String lib = travaux.getLibelle() != null ? travaux.getLibelle().trim().toUpperCase() : "";
            if (lib.endsWith("HT")) {
                throw new IllegalArgumentException("Un devis BT ne peut pas contenir des travaux HT.");
            }
        }
    }

    private void applyParticipants(PlanningTravaux planning, PlanningTravauxRequestDTO dto) {
        if (dto.getParticipantIds() != null) {
            planning.getParticipants().clear();
            planning.getParticipants().addAll(agentRepo.findAllById(dto.getParticipantIds()));
        }
        if (dto.getParticipantNames() != null) {
            planning.getParticipantsExternes().clear();
            dto.getParticipantNames().stream()
                    .filter(n -> n != null && !n.isBlank())
                    .map(String::trim)
                    .forEach(name -> planning.getParticipantsExternes().add(resolveParticipant(name)));
        }
    }

    private void validateScheduleConflicts(PlanningTravaux planning, String excludePlanningId) {
        if (planning.getDateDebut() == null) return;
        LocalDateTime end = effectiveEnd(planning);
        java.util.LinkedHashSet<Agent> members = new java.util.LinkedHashSet<>();
        if (planning.getAgent() != null) members.add(planning.getAgent());
        if (planning.getParticipants() != null) members.addAll(planning.getParticipants());

        for (Agent member : members) {
            if (!member.isActif()) {
                throw new IllegalStateException("L'agent " + member.getPrenom() + " " + member.getNom() + " est inactif.");
            }
            List<PlanningTravaux> conflicts = planningRepo.findOpenForAgent(member.getId()).stream()
                    .filter(c -> excludePlanningId == null || !excludePlanningId.equals(c.getId()))
                    .filter(c -> c.getDateDebut() != null && overlaps(planning.getDateDebut(), end, c.getDateDebut(), effectiveEnd(c)))
                    .toList();
            if (!conflicts.isEmpty()) {
                PlanningTravaux c = conflicts.get(0);
                String code = c.getDevis() != null ? c.getDevis().getDevisCode() : "autre devis";
                throw new IllegalStateException("Conflit de planning pour " + member.getPrenom() + " " + member.getNom()
                        + " : déjà programmé sur " + code + " à partir du " + c.getDateDebut() + ".");
            }
        }
    }

    private boolean overlaps(LocalDateTime aStart, LocalDateTime aEnd, LocalDateTime bStart, LocalDateTime bEnd) {
        return aStart.isBefore(bEnd) && bStart.isBefore(aEnd);
    }

    private LocalDateTime effectiveEnd(PlanningTravaux planning) {
        LocalDateTime start = planning.getDateDebut();
        return planning.getDateFin() != null && planning.getDateFin().isAfter(start) ? planning.getDateFin() : start.plusHours(8);
    }

    private void notifyPlanningMembers(PlanningTravaux planning, String title, String message) {
        java.util.LinkedHashSet<String> ids = new java.util.LinkedHashSet<>();
        if (planning.getAgent() != null) ids.add(planning.getAgent().getId());
        if (planning.getParticipants() != null) planning.getParticipants().forEach(a -> ids.add(a.getId()));
        String link = "/planning/" + planning.getDevis().getId();
        ids.forEach(id -> notificationService.notifyAgent(id, title, message, "PLANNING", link));
    }

    private Participant resolveParticipant(String fullName) {
        String[] parts = fullName.split(" ", 2);
        String nom    = parts[0];
        String prenom = parts.length == 2 ? parts[1] : "";
        return participantRepo.findByNomAndPrenom(nom, prenom)
                .orElseGet(() -> {
                    Participant p = new Participant();
                    p.setNom(nom);
                    p.setPrenom(prenom);
                    p.setExterne(true);
                    return participantRepo.save(p);
                });
    }

    @Transactional(readOnly = true)
    public List<PvDocumentDTO> getPvDocs(String planningId) {
        return documentRepository.findByPlanning_IdAndTypeDocument(planningId, TypeDocument.PV)
                .stream().map(PvDocumentDTO::fromEntity).toList();
    }

    @Transactional
    public PvDocumentDTO uploadPvDoc(String planningId, MultipartFile file, String pvKind, String observation, String participantsJson) throws IOException {
        PlanningTravaux planning = findById(planningId);
        String filePath = fileStorageService.store(file, "pv");
        Document doc = new Document();
        doc.setPlanning(planning);
        doc.setTypeDocument(TypeDocument.PV);
        doc.setFileName(file.getOriginalFilename());
        doc.setFilePath(filePath);
        doc.setMimeType(file.getContentType());
        doc.setObservation(observation);
        doc.setParticipantsJson(participantsJson);
        try {
            doc.setPvKind(PvKind.valueOf(pvKind));
        } catch (IllegalArgumentException ignored) {}
        return PvDocumentDTO.fromEntity(documentRepository.save(doc));
    }

    @Transactional
    public void deletePvDoc(String planningId, String docId) {
        Document doc = documentRepository.findById(docId)
                .orElseThrow(() -> new EntityNotFoundException("Document PV non trouvé : " + docId));
        if (doc.getPlanning() == null || !planningId.equals(doc.getPlanning().getId())) {
            throw new IllegalArgumentException("Ce document n'appartient pas au planning spécifié");
        }
        if (doc.getFilePath() != null && !doc.getFilePath().isBlank()) {
            try {
                Files.deleteIfExists(Path.of(doc.getFilePath()));
            } catch (IOException e) {
                log.warn("Impossible de supprimer le fichier PV physique : {}", doc.getFilePath(), e);
            }
        }
        documentRepository.delete(doc);
    }

    @Transactional(readOnly = true)
    public byte[] downloadPvDoc(String docId) throws IOException {
        Document doc = documentRepository.findById(docId)
                .orElseThrow(() -> new EntityNotFoundException("Document PV non trouvé : " + docId));
        try {
            return fileStorageService.load(doc.getFilePath());
        } catch (java.nio.file.NoSuchFileException e) {
            throw new EntityNotFoundException("Fichier introuvable sur le disque");
        }
    }

    private boolean isHt(String demOption) {
        return demOption != null && demOption.trim().toUpperCase().endsWith("HT");
    }
}