package com.gescli.ProgrammationTravaux.service;

import com.gescli.ProgrammationTravaux.dto.MaterielDTO;
import com.gescli.ProgrammationTravaux.entity.Materiel;
import com.gescli.ProgrammationTravaux.mapper.MaterielMapper;
import com.gescli.ProgrammationTravaux.repository.MaterielRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MaterielService {

    private final MaterielRepository repository;
    private final MaterielMapper mapper;

    @Transactional(readOnly = true)
    public List<MaterielDTO> getAll() {
        return repository.findAll().stream().map(mapper::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public MaterielDTO getById(String id) {
        return mapper.toDto(repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Matériel introuvable : " + id)));
    }

    @Transactional
    public MaterielDTO create(MaterielDTO dto) {
        Materiel entity = mapper.toEntity(dto);
        return mapper.toDto(repository.save(entity));
    }

    @Transactional
    public MaterielDTO update(String id, MaterielDTO dto) {
        Materiel entity = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Matériel introuvable : " + id));
        mapper.updateFromDto(dto, entity);
        return mapper.toDto(repository.save(entity));
    }

    @Transactional
    public void delete(String id) {
        if (!repository.existsById(id)) {
            throw new EntityNotFoundException("Matériel introuvable : " + id);
        }
        repository.deleteById(id);
    }
}
