package com.gescli.ProgrammationTravaux.controller;

import com.gescli.ProgrammationTravaux.dto.StructureDTO;
import com.gescli.ProgrammationTravaux.service.StructureService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/structures")
@RequiredArgsConstructor
public class StructureController {

    private final StructureService service;

    @GetMapping
    public ResponseEntity<List<StructureDTO>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    @PostMapping
    public ResponseEntity<StructureDTO> create(@RequestBody StructureDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<StructureDTO> update(@PathVariable String id, @RequestBody StructureDTO dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
