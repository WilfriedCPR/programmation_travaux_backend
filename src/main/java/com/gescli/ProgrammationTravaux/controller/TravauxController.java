package com.gescli.ProgrammationTravaux.controller;

import com.gescli.ProgrammationTravaux.dto.TravauxDTO;
import com.gescli.ProgrammationTravaux.service.TravauxService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/travaux")
@RequiredArgsConstructor
public class TravauxController {

    private final TravauxService service;

    @GetMapping
    public ResponseEntity<List<TravauxDTO>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TravauxDTO> getById(@PathVariable String id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @PostMapping
    public ResponseEntity<TravauxDTO> create(@Valid @RequestBody TravauxDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TravauxDTO> update(@PathVariable String id, @Valid @RequestBody TravauxDTO dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
