package com.gescli.ProgrammationTravaux.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gescli.ProgrammationTravaux.dto.ReferenceDTO;
import com.gescli.ProgrammationTravaux.entity.Reference;
import com.gescli.ProgrammationTravaux.mapper.ReferenceMapper;
import com.gescli.ProgrammationTravaux.service.ReferenceService;

import lombok.RequiredArgsConstructor;

@RestController
@CrossOrigin(origins = "http://localhost:4200")
@RequestMapping("/api/references")
@RequiredArgsConstructor
public class ReferenceController {

    private final ReferenceService referenceService;
    private final ReferenceMapper referenceMapper;

    @GetMapping
    public ResponseEntity<List<ReferenceDTO>> getAll() {
        List<Reference> refs = referenceService.getAllReferences();
        return ResponseEntity.ok(referenceMapper.toDTOs(refs));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReferenceDTO> getById(@PathVariable String id) {
        Reference ref = referenceService.getReferenceById(id);
        return ResponseEntity.ok(referenceMapper.toDto(ref));
    }

    @PostMapping
    public ResponseEntity<ReferenceDTO> create(@RequestBody ReferenceDTO dto) {
        Reference ref = referenceMapper.toEntity(dto);
        Reference saved = referenceService.createReference(ref);
        return ResponseEntity.status(HttpStatus.CREATED).body(referenceMapper.toDto(saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ReferenceDTO> update(@PathVariable String id, @RequestBody ReferenceDTO dto) {
        Reference ref = referenceMapper.toEntity(dto);
        Reference updated = referenceService.updateReference(id, ref);
        return ResponseEntity.ok(referenceMapper.toDto(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        referenceService.deleteReference(id);
        return ResponseEntity.noContent().build();
    }
}
