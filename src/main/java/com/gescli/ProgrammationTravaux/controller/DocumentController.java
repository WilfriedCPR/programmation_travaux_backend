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

import com.gescli.ProgrammationTravaux.dto.DocumentDTO;
import com.gescli.ProgrammationTravaux.entity.Document;
import com.gescli.ProgrammationTravaux.mapper.DocumentMapper;
import com.gescli.ProgrammationTravaux.service.DocumentService;

import lombok.RequiredArgsConstructor;

@RestController
@CrossOrigin(origins = "http://localhost:4200")
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;
    private final DocumentMapper documentMapper;

    @GetMapping
    public ResponseEntity<List<DocumentDTO>> getAll() {
        return ResponseEntity.ok(
            documentService.getAllDocuments().stream()
                .map(documentMapper::toDto)
                .collect(java.util.stream.Collectors.toList()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DocumentDTO> getById(@PathVariable String id) {
        return ResponseEntity.ok(documentMapper.toDto(documentService.getDocumentById(id)));
    }

    @PostMapping
    public ResponseEntity<DocumentDTO> create(@RequestBody DocumentDTO dto) {
        Document saved = documentService.createDocument(documentMapper.toEntity(dto));
        return ResponseEntity.status(HttpStatus.CREATED).body(documentMapper.toDto(saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DocumentDTO> update(@PathVariable String id, @RequestBody DocumentDTO dto) {
        Document updated = documentService.updateDocument(id, documentMapper.toEntity(dto));
        return ResponseEntity.ok(documentMapper.toDto(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        documentService.deleteDocument(id);
        return ResponseEntity.noContent().build();
    }
}
