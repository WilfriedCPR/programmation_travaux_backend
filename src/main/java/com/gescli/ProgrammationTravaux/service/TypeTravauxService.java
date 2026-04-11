package com.gescli.ProgrammationTravaux.service;

import com.gescli.ProgrammationTravaux.dto.TypeTravauxDTO;
import com.gescli.ProgrammationTravaux.entity.TypeTravaux;
import com.gescli.ProgrammationTravaux.mapper.TypeTravauxMapper;
import com.gescli.ProgrammationTravaux.repository.TypeTravauxRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TypeTravauxService {

    private final TypeTravauxRepository repository;
    private final TypeTravauxMapper mapper;

    @Transactional(readOnly = true)
    public List<TypeTravauxDTO> getAll() {
        return repository.findAll().stream().map(mapper::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TypeTravauxDTO getById(String id) {
        return mapper.toDto(repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("TypeTravaux introuvable : " + id)));
    }

    @Transactional
    public TypeTravauxDTO create(TypeTravauxDTO dto) {
        TypeTravaux entity = mapper.toEntity(dto);
        return mapper.toDto(repository.save(entity));
    }

    @Transactional
    public TypeTravauxDTO update(String id, TypeTravauxDTO dto) {
        TypeTravaux entity = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("TypeTravaux introuvable : " + id));
        entity.setLibelle(dto.getLibelle());
        return mapper.toDto(repository.save(entity));
    }

    @Transactional
    public void delete(String id) {
        if (!repository.existsById(id)) {
            throw new EntityNotFoundException("TypeTravaux introuvable : " + id);
        }
        repository.deleteById(id);
    }
}
