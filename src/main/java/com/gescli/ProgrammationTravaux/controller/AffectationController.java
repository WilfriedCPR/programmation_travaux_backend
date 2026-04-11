package com.gescli.ProgrammationTravaux.controller;

import com.gescli.ProgrammationTravaux.dto.AffectationRequestDTO;
import com.gescli.ProgrammationTravaux.dto.AffectationResponseDTO;
import com.gescli.ProgrammationTravaux.service.AffectationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/affectations")
@RequiredArgsConstructor
public class AffectationController {

    private final AffectationService affectationService;

    @PostMapping("/affecter")
    public ResponseEntity<AffectationResponseDTO> affecter(@RequestBody AffectationRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(affectationService.affecterAgent(dto));
    }

    @PutMapping("/retirer/{devisId}/{agentId}")
    public ResponseEntity<Void> retirer(@PathVariable String devisId, @PathVariable String agentId) {
        affectationService.retirerAgent(devisId, agentId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/reactiver/{devisId}/{agentId}")
    public ResponseEntity<Void> reactiver(@PathVariable String devisId, @PathVariable String agentId) {
        affectationService.reactiverAgent(devisId, agentId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/byDevis/{devisId}")
    public ResponseEntity<List<AffectationResponseDTO>> getByDevis(@PathVariable String devisId) {
        return ResponseEntity.ok(affectationService.getAgentsByDevisId(devisId));
    }

    @GetMapping("/byAgent/{agentId}")
    public ResponseEntity<List<AffectationResponseDTO>> getByAgent(@PathVariable String agentId) {
        return ResponseEntity.ok(affectationService.getDevisByAgentId(agentId));
    }
}