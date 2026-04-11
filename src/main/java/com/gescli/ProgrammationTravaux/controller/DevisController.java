package com.gescli.ProgrammationTravaux.controller;

import com.gescli.ProgrammationTravaux.dto.DevisClientInfoDTO;
import com.gescli.ProgrammationTravaux.dto.DevisRequestDTO;
import com.gescli.ProgrammationTravaux.dto.DevisResponseDTO;
import com.gescli.ProgrammationTravaux.service.DevisClientInfoService;
import com.gescli.ProgrammationTravaux.service.DevisService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/devis")
@RequiredArgsConstructor
public class DevisController {

    private final DevisService devisService;
    private final DevisClientInfoService devisClientInfoService;

    @GetMapping
    public ResponseEntity<Page<DevisResponseDTO>> getAll(
            Pageable pageable,
            @RequestParam(required = false) String statut,
            @RequestParam(required = false) String q) {
        return ResponseEntity.ok(devisService.getAllDevis(pageable, statut, q));
    }

    @GetMapping("/all")
    public ResponseEntity<List<DevisResponseDTO>> getAllList(
            @RequestParam(required = false) String statut,
            @RequestParam(required = false) String q) {
        return ResponseEntity.ok(devisService.getAllDevisList(statut, q));
    }

    @PostMapping
    public ResponseEntity<DevisResponseDTO> create(@Valid @RequestBody DevisRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(devisService.createDevis(dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DevisResponseDTO> getById(@PathVariable String id) {
        return ResponseEntity.ok(devisService.getDevisById(id));
    }

    @GetMapping("/{id}/client-info")
    public ResponseEntity<DevisClientInfoDTO> getClientInfo(@PathVariable String id) {
        DevisClientInfoDTO dto = devisClientInfoService.getByDevisId(id)
                .orElseGet(() -> { DevisClientInfoDTO d = new DevisClientInfoDTO(); d.setFound(false); return d; });
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/by-code/{devisCode}/client-info")
    public ResponseEntity<DevisClientInfoDTO> getClientInfoByCode(@PathVariable String devisCode) {
        DevisClientInfoDTO dto = devisClientInfoService.getByDevisCode(devisCode)
                .orElseGet(() -> { DevisClientInfoDTO d = new DevisClientInfoDTO(); d.setFound(false); return d; });
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/{id}")
    public ResponseEntity<DevisResponseDTO> update(@PathVariable String id,
                                                    @Valid @RequestBody DevisRequestDTO dto) {
        return ResponseEntity.ok(devisService.updateDevis(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        devisService.deleteDevis(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/hard")
    public ResponseEntity<Void> deleteHard(@PathVariable String id) {
        devisService.deleteDevisHard(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/has-bsm")
    public ResponseEntity<Boolean> hasBonSortie(@PathVariable String id) {
        return ResponseEntity.ok(devisService.hasBonSortie(id));
    }

    @PatchMapping("/{id}/close")
    public ResponseEntity<DevisResponseDTO> close(@PathVariable String id,
                                                   @RequestBody(required = false) Map<String, Object> payload) {
        String observation = payload != null ? (String) payload.get("observation") : null;
        return ResponseEntity.ok(devisService.closeDevis(id, observation));
    }
}