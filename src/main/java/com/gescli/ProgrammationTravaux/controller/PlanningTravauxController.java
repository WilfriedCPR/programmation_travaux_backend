package com.gescli.ProgrammationTravaux.controller;

import com.gescli.ProgrammationTravaux.dto.PlanningTravauxRequestDTO;
import com.gescli.ProgrammationTravaux.dto.PlanningTravauxResponseDTO;
import com.gescli.ProgrammationTravaux.dto.PvDocumentDTO;
import com.gescli.ProgrammationTravaux.mapper.PlanningTravauxMapper;
import com.gescli.ProgrammationTravaux.service.PlanningTravauxService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/plannings")
@RequiredArgsConstructor
public class PlanningTravauxController {

    private final PlanningTravauxService service;

    @GetMapping
    public ResponseEntity<List<PlanningTravauxResponseDTO>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PlanningTravauxResponseDTO> getById(@PathVariable String id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @GetMapping("/devis/{devisId}")
    public ResponseEntity<List<PlanningTravauxResponseDTO>> getByDevis(@PathVariable String devisId) {
        return ResponseEntity.ok(service.getByDevis(devisId));
    }

    @GetMapping("/devis/{devisId}/deleted")
    public ResponseEntity<List<PlanningTravauxResponseDTO>> getDeletedByDevis(@PathVariable String devisId) {
        return ResponseEntity.ok(service.getDeletedByDevis(devisId));
    }

    @GetMapping("/devis/{devisId}/closed")
    public ResponseEntity<List<PlanningTravauxResponseDTO>> getClosedByDevis(@PathVariable String devisId) {
        return ResponseEntity.ok(service.getClosedByDevis(devisId));
    }

    @PostMapping
    public ResponseEntity<PlanningTravauxResponseDTO> create(@RequestBody PlanningTravauxRequestDTO dto) {
        return ResponseEntity.ok(service.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PlanningTravauxResponseDTO> update(@PathVariable String id,
                                                              @RequestBody PlanningTravauxRequestDTO dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/hard")
    public ResponseEntity<Void> hardDelete(@PathVariable String id) {
        service.hardDelete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/close")
    public ResponseEntity<PlanningTravauxResponseDTO> close(@PathVariable String id) {
        return ResponseEntity.ok(PlanningTravauxMapper.toDTO(service.closePlanning(id)));
    }

    @PutMapping("/{id}/cloture")
    public ResponseEntity<PlanningTravauxResponseDTO> cloture(@PathVariable String id) {
        return ResponseEntity.ok(PlanningTravauxMapper.toDTO(service.closePlanning(id)));
    }

    @GetMapping("/{id}/pv-docs")
    public ResponseEntity<List<PvDocumentDTO>> getPvDocs(@PathVariable String id) {
        return ResponseEntity.ok(service.getPvDocs(id));
    }

    @PostMapping(value = "/{id}/pv-docs", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PvDocumentDTO> uploadPvDoc(
            @PathVariable String id,
            @RequestParam MultipartFile file,
            @RequestParam String pvKind,
            @RequestParam(required = false) String observation) throws IOException {
        return ResponseEntity.ok(service.uploadPvDoc(id, file, pvKind, observation));
    }

    @DeleteMapping("/{id}/pv-docs/{docId}")
    public ResponseEntity<Void> deletePvDoc(@PathVariable String id, @PathVariable String docId) {
        service.deletePvDoc(id, docId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/pv-docs/{docId}/download")
    public ResponseEntity<byte[]> downloadPvDoc(@PathVariable String docId) throws IOException {
        byte[] data = service.downloadPvDoc(docId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"pv-" + docId + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(data);
    }
}