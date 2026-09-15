package com.gescli.ProgrammationTravaux.service;

import com.gescli.ProgrammationTravaux.dto.StructureDTO;
import com.gescli.ProgrammationTravaux.entity.Structure;
import com.gescli.ProgrammationTravaux.mapper.StructureMapper;
import com.gescli.ProgrammationTravaux.repository.StructureRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StructureService {

    private static final String SONABEL = "SONABEL";

    private final StructureRepository repository;
    private final StructureMapper mapper;

    @Transactional(readOnly = true)
    public List<StructureDTO> getAll() {
        return repository.findAll().stream().map(mapper::toDto).collect(Collectors.toList());
    }

    @Transactional
    public StructureDTO create(StructureDTO dto) {
        String libelle = normalize(dto.getLibelle());
        repository.findFirstByLibelleIgnoreCase(libelle).ifPresent(existing -> {
            throw new IllegalArgumentException("Cette structure existe déjà : " + existing.getLibelle());
        });

        Structure entity = mapper.toEntity(dto);
        entity.setLibelle(libelle);
        return mapper.toDto(repository.save(entity));
    }

    @Transactional
    public StructureDTO update(String id, StructureDTO dto) {
        Structure entity = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Structure introuvable : " + id));

        String libelle = normalize(dto.getLibelle());
        if (SONABEL.equalsIgnoreCase(entity.getLibelle()) && !SONABEL.equalsIgnoreCase(libelle)) {
            throw new IllegalStateException("La structure SONABEL est une structure système et ne peut pas être renommée.");
        }

        repository.findFirstByLibelleIgnoreCase(libelle)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Cette structure existe déjà : " + existing.getLibelle());
                });

        entity.setLibelle(SONABEL.equalsIgnoreCase(libelle) ? SONABEL : libelle);
        return mapper.toDto(repository.save(entity));
    }

    @Transactional
    public void delete(String id) {
        Structure entity = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Structure introuvable : " + id));

        if (SONABEL.equalsIgnoreCase(entity.getLibelle())) {
            throw new IllegalStateException("La structure SONABEL est une structure système et ne peut pas être supprimée.");
        }
        repository.delete(entity);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
