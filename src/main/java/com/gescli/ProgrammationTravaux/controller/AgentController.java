package com.gescli.ProgrammationTravaux.controller;

import java.util.List;

import org.springframework.data.domain.Pageable;
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

import com.gescli.ProgrammationTravaux.dto.AgentRequestDTO;
import com.gescli.ProgrammationTravaux.dto.AgentResponseDTO;
import com.gescli.ProgrammationTravaux.service.AgentService;

import lombok.RequiredArgsConstructor;

@RestController
@CrossOrigin(origins = "http://localhost:4200")
@RequestMapping("/api/agents")
@RequiredArgsConstructor
public class AgentController {

    private final AgentService agentService;

    @GetMapping
    public ResponseEntity<List<AgentResponseDTO>> getAll() {
        return ResponseEntity.ok(agentService.getAllAgents(Pageable.unpaged()).getContent());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AgentResponseDTO> getById(@PathVariable String id) {
        return ResponseEntity.ok(agentService.getAgentById(id));
    }

    @PostMapping
    public ResponseEntity<AgentResponseDTO> create(@RequestBody AgentRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(agentService.createAgent(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AgentResponseDTO> update(@PathVariable String id, @RequestBody AgentRequestDTO dto) {
        return ResponseEntity.ok(agentService.updateAgent(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        agentService.deleteAgent(id);
        return ResponseEntity.noContent().build();
    }
}
