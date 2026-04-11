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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.gescli.ProgrammationTravaux.dto.TravauxContrainteRequestDTO;
import com.gescli.ProgrammationTravaux.dto.TravauxContrainteResponseDTO;
import com.gescli.ProgrammationTravaux.service.TravauxContrainteService;

import lombok.RequiredArgsConstructor;

@RestController
@CrossOrigin(origins = "http://localhost:4200")
@RequestMapping("/api/contraintes")
@RequiredArgsConstructor
public class TravauxContrainteController {

    private final TravauxContrainteService travauxContrainteService;

    @GetMapping("/travaux/{travauxId}")
    public ResponseEntity<List<TravauxContrainteResponseDTO>> getByTravaux(@PathVariable String travauxId) {
        return ResponseEntity.ok(travauxContrainteService.getContraintesByTravail(travauxId));
    }

    @GetMapping("/planning/{planningId}")
    public ResponseEntity<List<TravauxContrainteResponseDTO>> getByPlanning(@PathVariable String planningId) {
        return ResponseEntity.ok(travauxContrainteService.getContraintesByPlanning(planningId));
    }

    @GetMapping
    public ResponseEntity<List<TravauxContrainteResponseDTO>> getAll() {
        return ResponseEntity.ok(travauxContrainteService.getAllContraintes());
    }

    @PostMapping
    public ResponseEntity<TravauxContrainteResponseDTO> addToTravaux(@RequestBody TravauxContrainteRequestDTO dto) {
        TravauxContrainteResponseDTO saved = travauxContrainteService.addContrainteToTravail(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PostMapping("/planning/{planningId}")
    public ResponseEntity<TravauxContrainteResponseDTO> addToPlanning(
            @PathVariable String planningId,
            @RequestBody TravauxContrainteRequestDTO dto) {
        TravauxContrainteResponseDTO saved = travauxContrainteService.addContrainteToPlanning(
                planningId, dto.getContrainteLibelle(), dto.getObservationMise());
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TravauxContrainteResponseDTO> update(
            @PathVariable String id,
            @RequestBody TravauxContrainteRequestDTO dto) {
        return ResponseEntity.ok(travauxContrainteService.update(id, dto));
    }

    @PutMapping("/{id}/lever")
    public ResponseEntity<TravauxContrainteResponseDTO> lever(
            @PathVariable String id,
            @RequestParam(required = false) String observationLevee) {
        return ResponseEntity.ok(travauxContrainteService.removeContrainteFromTravail(id, observationLevee));
    }

    @PutMapping("/planning/{planningId}/contrainte/{contrainteId}/lever")
    public ResponseEntity<TravauxContrainteResponseDTO> leverForPlanning(
            @PathVariable String planningId,
            @PathVariable String contrainteId,
            @RequestParam(required = false) String observationLevee) {
        return ResponseEntity.ok(travauxContrainteService.leverContrainte(planningId, contrainteId, observationLevee));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        travauxContrainteService.deleteContrainte(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/suggestions")
    public ResponseEntity<List<String>> suggest(@RequestParam(required = false) String query) {
        return ResponseEntity.ok(travauxContrainteService.suggestLibelles(query));
    }
}
