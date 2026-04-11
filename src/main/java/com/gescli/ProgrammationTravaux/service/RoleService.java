package com.gescli.ProgrammationTravaux.service;

import com.gescli.ProgrammationTravaux.dto.RoleDTO;
import com.gescli.ProgrammationTravaux.entity.Role;
import com.gescli.ProgrammationTravaux.mapper.RoleMapper;
import com.gescli.ProgrammationTravaux.repository.RoleRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository repository;
    private final RoleMapper mapper;

    @Transactional(readOnly = true)
    public List<RoleDTO> getAll() {
        return repository.findAll().stream().map(mapper::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RoleDTO getById(String id) {
        return mapper.toDto(repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Rôle introuvable : " + id)));
    }

    @Transactional
    public RoleDTO create(RoleDTO dto) {
        Role role = mapper.toEntity(dto);
        return mapper.toDto(repository.save(role));
    }

    @Transactional
    public RoleDTO update(String id, RoleDTO dto) {
        Role role = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Rôle introuvable : " + id));
        role.setLibelle(dto.getLibelle());
        return mapper.toDto(repository.save(role));
    }

    @Transactional
    public void delete(String id) {
        if (!repository.existsById(id)) {
            throw new EntityNotFoundException("Rôle introuvable : " + id);
        }
        repository.deleteById(id);
    }
}
