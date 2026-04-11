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

    private final StructureRepository repository;
    private final StructureMapper mapper;

    @Transactional(readOnly = true)
    public List<StructureDTO> getAll() {
        return repository.findAll().stream().map(mapper::toDto).collect(Collectors.toList());
    }

    @Transactional
    public StructureDTO create(StructureDTO dto) {
        Structure entity = mapper.toEntity(dto);
        return mapper.toDto(repository.save(entity));
    }

    @Transactional
    public StructureDTO update(String id, StructureDTO dto) {
        Structure entity = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Structure introuvable : " + id));
        entity.setLibelle(dto.getLibelle());
        return mapper.toDto(repository.save(entity));
    }

    @Transactional
    public void delete(String id) {
        if (!repository.existsById(id)) {
            throw new EntityNotFoundException("Structure introuvable : " + id);
        }
        repository.deleteById(id);
    }
}
