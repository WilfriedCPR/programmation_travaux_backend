package com.gescli.ProgrammationTravaux.controller;

import com.gescli.ProgrammationTravaux.dto.ParticipantDTO;
import com.gescli.ProgrammationTravaux.service.ParticipantService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/participants")
@RequiredArgsConstructor
public class ParticipantController {

    private final ParticipantService service;

    @GetMapping
    public ResponseEntity<List<ParticipantDTO>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    @GetMapping("/externes")
    public ResponseEntity<List<ParticipantDTO>> getExternes() {
        return ResponseEntity.ok(service.getExternes());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ParticipantDTO> getById(@PathVariable String id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @PostMapping
    public ResponseEntity<ParticipantDTO> create(@RequestBody ParticipantDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ParticipantDTO> update(@PathVariable String id, @RequestBody ParticipantDTO dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
