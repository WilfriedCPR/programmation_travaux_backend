package com.gescli.ProgrammationTravaux.controller;

import com.gescli.ProgrammationTravaux.dto.TypeTravauxDTO;
import com.gescli.ProgrammationTravaux.service.TypeTravauxService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/type-travaux")
@RequiredArgsConstructor
public class TypeTravauxController {

    private final TypeTravauxService service;

    @GetMapping
    public ResponseEntity<List<TypeTravauxDTO>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TypeTravauxDTO> getById(@PathVariable String id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @PostMapping
    public ResponseEntity<TypeTravauxDTO> create(@Valid @RequestBody TypeTravauxDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TypeTravauxDTO> update(@PathVariable String id, @Valid @RequestBody TypeTravauxDTO dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
