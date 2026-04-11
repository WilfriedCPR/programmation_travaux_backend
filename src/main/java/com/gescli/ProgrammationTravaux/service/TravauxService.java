package com.gescli.ProgrammationTravaux.service;

import com.gescli.ProgrammationTravaux.dto.TravauxDTO;
import com.gescli.ProgrammationTravaux.entity.Travaux;
import com.gescli.ProgrammationTravaux.entity.TypeTravaux;
import com.gescli.ProgrammationTravaux.mapper.TravauxMapper;
import com.gescli.ProgrammationTravaux.repository.TravauxRepository;
import com.gescli.ProgrammationTravaux.repository.TypeTravauxRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TravauxService {

    private final TravauxRepository repository;
    private final TypeTravauxRepository typeTravauxRepository;
    private final TravauxMapper mapper;

    @Transactional(readOnly = true)
    public List<TravauxDTO> getAll() {
        return repository.findAll().stream().map(mapper::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TravauxDTO getById(String id) {
        return mapper.toDto(repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Travaux introuvable : " + id)));
    }

    @Transactional
    public TravauxDTO create(TravauxDTO dto) {
        Travaux entity = new Travaux();
        entity.setLibelle(dto.getLibelle());
        if (dto.getTypeTravauxId() != null) {
            TypeTravaux type = typeTravauxRepository.findById(dto.getTypeTravauxId())
                    .orElseThrow(() -> new EntityNotFoundException("TypeTravaux introuvable : " + dto.getTypeTravauxId()));
            entity.setTypeTravaux(type);
        } else if (dto.getTypeTravauxLibelle() != null) {
            TypeTravaux type = typeTravauxRepository.findByLibelle(dto.getTypeTravauxLibelle())
                    .orElseThrow(() -> new EntityNotFoundException("TypeTravaux introuvable : " + dto.getTypeTravauxLibelle()));
            entity.setTypeTravaux(type);
        }
        return mapper.toDto(repository.save(entity));
    }

    @Transactional
    public TravauxDTO update(String id, TravauxDTO dto) {
        Travaux entity = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Travaux introuvable : " + id));
        entity.setLibelle(dto.getLibelle());
        if (dto.getTypeTravauxId() != null) {
            TypeTravaux type = typeTravauxRepository.findById(dto.getTypeTravauxId())
                    .orElseThrow(() -> new EntityNotFoundException("TypeTravaux introuvable : " + dto.getTypeTravauxId()));
            entity.setTypeTravaux(type);
        } else if (dto.getTypeTravauxLibelle() != null) {
            TypeTravaux type = typeTravauxRepository.findByLibelle(dto.getTypeTravauxLibelle())
                    .orElseThrow(() -> new EntityNotFoundException("TypeTravaux introuvable : " + dto.getTypeTravauxLibelle()));
            entity.setTypeTravaux(type);
        }
        return mapper.toDto(repository.save(entity));
    }

    @Transactional
    public void delete(String id) {
        if (!repository.existsById(id)) {
            throw new EntityNotFoundException("Travaux introuvable : " + id);
        }
        repository.deleteById(id);
    }
}
