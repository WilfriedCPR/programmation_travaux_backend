package com.gescli.ProgrammationTravaux.service;

import com.gescli.ProgrammationTravaux.dto.ParticipantDTO;
import com.gescli.ProgrammationTravaux.entity.Participant;
import com.gescli.ProgrammationTravaux.mapper.ParticipantMapper;
import com.gescli.ProgrammationTravaux.repository.ParticipantRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ParticipantService {

    private final ParticipantRepository repository;
    private final ParticipantMapper mapper;

    @Transactional(readOnly = true)
    public List<ParticipantDTO> getAll() {
        return repository.findAll().stream().map(mapper::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ParticipantDTO> getExternes() {
        return repository.findByExterneTrue().stream().map(mapper::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ParticipantDTO getById(String id) {
        return mapper.toDto(repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Participant introuvable : " + id)));
    }

    @Transactional
    public ParticipantDTO create(ParticipantDTO dto) {
        Participant entity = mapper.toEntity(dto);
        // Les participants gérés depuis ce module sont externes à la SONABEL.
        entity.setExterne(true);
        return mapper.toDto(repository.save(entity));
    }

    @Transactional
    public ParticipantDTO update(String id, ParticipantDTO dto) {
        Participant entity = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Participant introuvable : " + id));
        mapper.updateFromDto(dto, entity);
        entity.setExterne(true);
        return mapper.toDto(repository.save(entity));
    }

    @Transactional
    public void delete(String id) {
        if (!repository.existsById(id)) {
            throw new EntityNotFoundException("Participant introuvable : " + id);
        }
        repository.deleteById(id);
    }
}
