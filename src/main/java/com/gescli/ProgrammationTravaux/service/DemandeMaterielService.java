package com.gescli.ProgrammationTravaux.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.gescli.ProgrammationTravaux.dto.DemandeMaterielDTO;
import com.gescli.ProgrammationTravaux.dto.LigneDemandeDTO;
import com.gescli.ProgrammationTravaux.entity.DemandeMateriel;
import com.gescli.ProgrammationTravaux.entity.DemandeMaterielLigne;
import com.gescli.ProgrammationTravaux.entity.Devis;
import com.gescli.ProgrammationTravaux.entity.Materiel;
import com.gescli.ProgrammationTravaux.repository.DemandeMaterielRepository;
import com.gescli.ProgrammationTravaux.repository.DevisRepository;
import com.gescli.ProgrammationTravaux.repository.MaterielRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DemandeMaterielService {

    private final DemandeMaterielRepository demandeMaterielRepository;
    private final DevisRepository devisRepository;
    private final MaterielRepository materielRepository;

    @Transactional
    public DemandeMateriel save(DemandeMateriel demande) {
        return demandeMaterielRepository.save(demande);
    }

    @Transactional(readOnly = true)
    public List<DemandeMateriel> getAll() {
        return demandeMaterielRepository.findByDeletedFalse();
    }

    @Transactional(readOnly = true)
    public List<DemandeMateriel> getByDevisId(String devisId) {
        return demandeMaterielRepository.findByDevisIdAndDeletedFalse(devisId);
    }

    @Transactional(readOnly = true)
    public List<DemandeMateriel> getDeletedAll() {
        return demandeMaterielRepository.findByDeletedTrue();
    }

    @Transactional(readOnly = true)
    public List<DemandeMateriel> getDeletedByDevisId(String devisId) {
        return demandeMaterielRepository.findByDevisIdAndDeletedTrue(devisId);
    }

    @Transactional(readOnly = true)
    public DemandeMateriel getById(String id) {
        return demandeMaterielRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Demande non trouvée avec l'ID : " + id));
    }

    @Transactional
    public void delete(String id) {
        DemandeMateriel d = getById(id);
        d.setDeleted(true);
        demandeMaterielRepository.save(d);
    }

    @Transactional
    public DemandeMateriel create(DemandeMaterielDTO dto) {
        if (!StringUtils.hasText(dto.getDevisId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le devis est obligatoire");
        }
        if (!StringUtils.hasText(dto.getReferenceValue())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La référence est obligatoire");
        }
        if (demandeMaterielRepository.existsByDevisIdAndReferenceValueAndDeletedFalse(dto.getDevisId(), dto.getReferenceValue())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Une demande possède déjà cette référence pour ce devis");
        }
        DemandeMateriel demande = new DemandeMateriel();
        applyDtoToEntity(dto, demande);
        demande.setDateDemande(java.time.LocalDateTime.now());
        return demandeMaterielRepository.save(demande);
    }

    @Transactional
    public DemandeMateriel update(String id, DemandeMaterielDTO dto) {
        if (!StringUtils.hasText(dto.getDevisId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le devis est obligatoire");
        }
        if (!StringUtils.hasText(dto.getReferenceValue())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La référence est obligatoire");
        }
        if (demandeMaterielRepository.existsByDevisIdAndReferenceValueAndIdNotAndDeletedFalse(dto.getDevisId(), dto.getReferenceValue(), id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Une demande possède déjà cette référence pour ce devis");
        }
        DemandeMateriel demande = getById(id);
        applyDtoToEntity(dto, demande);
        return demandeMaterielRepository.save(demande);
    }

    private void applyDtoToEntity(DemandeMaterielDTO dto, DemandeMateriel demande) {
        demande.setValide(dto.isValide());
        demande.setDateValidation(dto.getDateValidation());
        demande.setDateDemande(dto.getDateDemande());
        demande.setReferenceValue(dto.getReferenceValue());

        if (dto.getDevisId() != null) {
            Devis devis = devisRepository.findById(dto.getDevisId())
                    .orElseThrow(() -> new EntityNotFoundException("Devis introuvable: " + dto.getDevisId()));
            demande.setDevis(devis);
        }

        demande.getLignes().clear();
        if (dto.getLignes() != null) {
            for (LigneDemandeDTO l : dto.getLignes()) {
                DemandeMaterielLigne ent = new DemandeMaterielLigne();
                ent.setDemande(demande);
                ent.setQuantite(l.getQuantite());
                ent.setReference(l.getReference());
                if (l.getMaterielId() != null) {
                    Materiel mat = materielRepository.findById(l.getMaterielId())
                            .orElseThrow(() -> new EntityNotFoundException("Matériel introuvable: " + l.getMaterielId()));
                    ent.setMateriel(mat);
                }
                demande.getLignes().add(ent);
            }
        }
    }
}
