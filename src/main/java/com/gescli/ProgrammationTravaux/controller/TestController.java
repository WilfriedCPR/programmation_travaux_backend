package com.gescli.ProgrammationTravaux.controller;

import com.gescli.ProgrammationTravaux.service.TravauxContrainteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class TestController {
    
    private final TravauxContrainteService travauxContrainteService;

    @GetMapping("/test-contraintes")
    public ResponseEntity<List<String>> testContraintes() {
        return ResponseEntity.ok(travauxContrainteService.suggestLibelles(null));
    }
}
