package com.gescli.ProgrammationTravaux.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RestController;

import com.gescli.ProgrammationTravaux.dto.DemandeMaterielDTO;
import com.gescli.ProgrammationTravaux.entity.DemandeMateriel;
import com.gescli.ProgrammationTravaux.mapper.DemandeMaterielMapper;
import com.gescli.ProgrammationTravaux.service.DemandeMaterielService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/demandes-materiel")
@RequiredArgsConstructor
public class DemandeMaterielController {

    private final DemandeMaterielService demandeMaterielService;
    private final DemandeMaterielMapper demandeMaterielMapper;

    @GetMapping
    public ResponseEntity<List<DemandeMaterielDTO>> getAll() {
        List<DemandeMaterielDTO> dtos = demandeMaterielService.getAll().stream()
                .map(demandeMaterielMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/deleted")
    public ResponseEntity<List<DemandeMaterielDTO>> getDeleted() {
        List<DemandeMaterielDTO> dtos = demandeMaterielService.getDeletedAll().stream()
                .map(demandeMaterielMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/devis/{devisId}")
    public ResponseEntity<List<DemandeMaterielDTO>> getByDevis(@PathVariable String devisId) {
        List<DemandeMaterielDTO> dtos = demandeMaterielService.getByDevisId(devisId).stream()
                .map(demandeMaterielMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/devis/{devisId}/all")
    public ResponseEntity<List<DemandeMaterielDTO>> getByDevisAll(@PathVariable String devisId) {
        List<DemandeMateriel> active = demandeMaterielService.getByDevisId(devisId);
        List<DemandeMateriel> deleted = demandeMaterielService.getDeletedByDevisId(devisId);
        active.addAll(deleted);
        List<DemandeMaterielDTO> dtos = active.stream()
                .map(demandeMaterielMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DemandeMaterielDTO> getById(@PathVariable String id) {
        DemandeMateriel demande = demandeMaterielService.getById(id);
        return ResponseEntity.ok(demandeMaterielMapper.toDto(demande));
    }

    @PostMapping
    public ResponseEntity<DemandeMaterielDTO> create(@Valid @RequestBody DemandeMaterielDTO dto) {
        DemandeMateriel created = demandeMaterielService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(demandeMaterielMapper.toDto(created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DemandeMaterielDTO> update(@PathVariable String id, @Valid @RequestBody DemandeMaterielDTO dto) {
        DemandeMateriel updated = demandeMaterielService.update(id, dto);
        return ResponseEntity.ok(demandeMaterielMapper.toDto(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        demandeMaterielService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<java.util.Map<String, String>> handleIllegalArgumentException(IllegalArgumentException e) {
        java.util.Map<String, String> response = new java.util.HashMap<>();
        response.put("message", e.getMessage());
        return ResponseEntity.badRequest().body(response);
    }
}
