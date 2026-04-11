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

import com.gescli.ProgrammationTravaux.dto.OperationDTO;
import com.gescli.ProgrammationTravaux.entity.Operation;
import com.gescli.ProgrammationTravaux.mapper.OperationMapper;
import com.gescli.ProgrammationTravaux.service.OperationService;

import lombok.RequiredArgsConstructor;

@RestController
@CrossOrigin(origins = "http://localhost:4200")
@RequestMapping("/api/operations")
@RequiredArgsConstructor
public class OperationController {

    private final OperationService operationService;
    private final OperationMapper operationMapper;

    @GetMapping
    public ResponseEntity<List<OperationDTO>> getAll() {
        List<Operation> ops = operationService.getAll();
        return ResponseEntity.ok(operationMapper.toDTOs(ops));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OperationDTO> getById(@PathVariable String id) {
        Operation op = operationService.getById(id);
        return ResponseEntity.ok(operationMapper.toDto(op));
    }

    @PostMapping
    public ResponseEntity<OperationDTO> create(@RequestBody OperationDTO dto) {
        Operation op = operationMapper.toEntity(dto);
        Operation saved = operationService.save(op);
        return ResponseEntity.status(HttpStatus.CREATED).body(operationMapper.toDto(saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<OperationDTO> update(@PathVariable String id, @RequestBody OperationDTO dto) {
        Operation op = operationMapper.toEntity(dto);
        op.setId(id);
        Operation updated = operationService.save(op);
        return ResponseEntity.ok(operationMapper.toDto(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        operationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
