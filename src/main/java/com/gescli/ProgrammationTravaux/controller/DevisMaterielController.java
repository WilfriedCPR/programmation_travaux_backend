package com.gescli.ProgrammationTravaux.controller;

import java.util.List;
import java.util.stream.Collectors;

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

import com.gescli.ProgrammationTravaux.dto.DevisMaterielDTO;
import com.gescli.ProgrammationTravaux.entity.DevisMateriel;
import com.gescli.ProgrammationTravaux.mapper.DevisMaterielMapper;
import com.gescli.ProgrammationTravaux.service.DevisMaterielService;

import lombok.RequiredArgsConstructor;

@RestController
@CrossOrigin(origins = "http://localhost:4200")
@RequestMapping("/api/devis-materiels")
@RequiredArgsConstructor
public class DevisMaterielController {

    private final DevisMaterielService devisMaterielService;
    private final DevisMaterielMapper devisMaterielMapper;

    @GetMapping("/devis/{devisId}")
    public ResponseEntity<List<DevisMaterielDTO>> getByDevis(@PathVariable String devisId) {
        List<DevisMaterielDTO> dtos = devisMaterielService.getByDevisId(devisId).stream()
                .map(devisMaterielMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DevisMaterielDTO> getById(@PathVariable String id) {
        return ResponseEntity.ok(devisMaterielMapper.toDto(devisMaterielService.getById(id)));
    }

    @PostMapping
    public ResponseEntity<DevisMaterielDTO> create(@RequestBody DevisMaterielDTO dto) {
        DevisMateriel entity = devisMaterielMapper.toEntity(dto);
        if (dto.getDevisId() != null) {
            entity.setDevis(devisMaterielService.getDevis(dto.getDevisId()));
        }
        if (dto.getMaterielId() != null) {
            entity.setMateriel(devisMaterielService.getMateriel(dto.getMaterielId()));
        }
        DevisMateriel saved = devisMaterielService.save(entity);
        return ResponseEntity.status(HttpStatus.CREATED).body(devisMaterielMapper.toDto(saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DevisMaterielDTO> update(@PathVariable String id, @RequestBody DevisMaterielDTO dto) {
        DevisMateriel entity = devisMaterielMapper.toEntity(dto);
        if (dto.getDevisId() != null) {
            entity.setDevis(devisMaterielService.getDevis(dto.getDevisId()));
        }
        if (dto.getMaterielId() != null) {
            entity.setMateriel(devisMaterielService.getMateriel(dto.getMaterielId()));
        }
        DevisMateriel updated = devisMaterielService.update(id, entity);
        return ResponseEntity.ok(devisMaterielMapper.toDto(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        devisMaterielService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
