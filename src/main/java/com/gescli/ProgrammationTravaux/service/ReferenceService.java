package com.gescli.ProgrammationTravaux.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gescli.ProgrammationTravaux.entity.Reference;
import com.gescli.ProgrammationTravaux.repository.ReferenceRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReferenceService {

    private final ReferenceRepository referenceRepository;

    @Transactional
    public Reference createReference(Reference reference) {
        return referenceRepository.save(reference);
    }

    @Transactional(readOnly = true)
    public List<Reference> getAllReferences() {
        return referenceRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Reference getReferenceById(String id) {
        return referenceRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Référence non trouvée avec l'ID : " + id));
    }

    @Transactional
    public Reference updateReference(String id, Reference updated) {
        Reference reference = getReferenceById(id);
        reference.setReference(updated.getReference());
        reference.setTypeReference(updated.getTypeReference());
        return referenceRepository.save(reference);
    }

    @Transactional
    public void deleteReference(String id) {
        if (!referenceRepository.existsById(id)) {
            throw new EntityNotFoundException("Référence non trouvée avec l'ID : " + id);
        }
        referenceRepository.deleteById(id);
    }
}
