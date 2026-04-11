package com.gescli.ProgrammationTravaux.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gescli.ProgrammationTravaux.entity.Devis;
import com.gescli.ProgrammationTravaux.entity.DevisMateriel;
import com.gescli.ProgrammationTravaux.entity.Materiel;
import com.gescli.ProgrammationTravaux.repository.DevisMaterielRepository;
import com.gescli.ProgrammationTravaux.repository.DevisRepository;
import com.gescli.ProgrammationTravaux.repository.MaterielRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DevisMaterielService {

    private final DevisMaterielRepository devisMaterielRepository;
    private final DevisRepository devisRepository;
    private final MaterielRepository materielRepository;

    @Transactional(readOnly = true)
    public List<DevisMateriel> getAll() {
        return devisMaterielRepository.findAll();
    }

    @Transactional(readOnly = true)
    public DevisMateriel getById(String id) {
        return devisMaterielRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("DevisMateriel non trouvé avec l'ID : " + id));
    }

    @Transactional(readOnly = true)
    public List<DevisMateriel> getByDevisId(String devisId) {
        return devisMaterielRepository.findByDevisId(devisId);
    }

    @Transactional
    public DevisMateriel save(DevisMateriel devisMateriel) {
        return devisMaterielRepository.save(devisMateriel);
    }

    @Transactional
    public DevisMateriel update(String id, DevisMateriel updated) {
        DevisMateriel existing = getById(id);
        existing.setQuantity(updated.getQuantity());
        if (updated.getMateriel() != null) {
            existing.setMateriel(updated.getMateriel());
        }
        if (updated.getDevis() != null) {
            existing.setDevis(updated.getDevis());
        }
        return devisMaterielRepository.save(existing);
    }

    @Transactional
    public void delete(String id) {
        if (!devisMaterielRepository.existsById(id)) {
            throw new EntityNotFoundException("DevisMateriel non trouvé avec l'ID : " + id);
        }
        devisMaterielRepository.deleteById(id);
    }

    public Devis getDevis(String devisId) {
        return devisRepository.findById(devisId)
                .orElseThrow(() -> new EntityNotFoundException("Devis non trouvé avec l'ID : " + devisId));
    }

    public Materiel getMateriel(String materielId) {
        return materielRepository.findById(materielId)
                .orElseThrow(() -> new EntityNotFoundException("Matériel non trouvé avec l'ID : " + materielId));
    }
}
