package com.gescli.ProgrammationTravaux.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RestController;

import com.gescli.ProgrammationTravaux.dto.BonDeSortieMaterielDTO;
import com.gescli.ProgrammationTravaux.service.BonSortieMaterielService;
import com.gescli.ProgrammationTravaux.service.BonSortieMaterielService.BonsParDemande;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/bons-sortie")
@RequiredArgsConstructor
public class BonSortieMaterielController {

    private final BonSortieMaterielService bonSortieMaterielService;

    @GetMapping
    public ResponseEntity<List<BonDeSortieMaterielDTO>> getAll() {
        return ResponseEntity.ok(bonSortieMaterielService.getAllBonsSortie());
    }

    @GetMapping("/initiaux")
    public ResponseEntity<List<BonDeSortieMaterielDTO>> getBonsInitiaux() {
        return ResponseEntity.ok(bonSortieMaterielService.getBonsInitiaux());
    }

    @GetMapping("/{id}")
    public ResponseEntity<BonDeSortieMaterielDTO> getById(@PathVariable String id) {
        return ResponseEntity.ok(bonSortieMaterielService.getBonSortieById(id));
    }

    @GetMapping("/numero/{numeroBon}")
    public ResponseEntity<BonDeSortieMaterielDTO> getByNumero(@PathVariable String numeroBon) {
        return ResponseEntity.ok(bonSortieMaterielService.getBonSortieByNumero(numeroBon));
    }

    @GetMapping("/demande/{demandeMaterielId}")
    public ResponseEntity<BonsParDemande> getByDemande(@PathVariable String demandeMaterielId) {
        return ResponseEntity.ok(bonSortieMaterielService.listByDemande(demandeMaterielId));
    }

    @GetMapping("/demande/{demandeMaterielId}/initial")
    public ResponseEntity<BonDeSortieMaterielDTO> getInitialByDemande(@PathVariable String demandeMaterielId) {
        return ResponseEntity.ok(bonSortieMaterielService.getInitialByDemande(demandeMaterielId));
    }

    @GetMapping("/{bonInitialId}/complementaires")
    public ResponseEntity<List<BonDeSortieMaterielDTO>> getComplementaires(@PathVariable String bonInitialId) {
        return ResponseEntity.ok(bonSortieMaterielService.getBonsComplementaires(bonInitialId));
    }

    @GetMapping("/client")
    public ResponseEntity<List<BonDeSortieMaterielDTO>> getByClient(@RequestParam String nom) {
        return ResponseEntity.ok(bonSortieMaterielService.getBonsByClient(nom));
    }

    @GetMapping("/devis/{numeroDevis}")
    public ResponseEntity<List<BonDeSortieMaterielDTO>> getByDevis(@PathVariable String numeroDevis) {
        return ResponseEntity.ok(bonSortieMaterielService.getBonsByDevis(numeroDevis));
    }

    @GetMapping("/by-devis-id/{devisId}")
    public ResponseEntity<List<BonDeSortieMaterielDTO>> getByDevisId(@PathVariable String devisId) {
        return ResponseEntity.ok(bonSortieMaterielService.getBonsByDevisId(devisId));
    }

    @GetMapping("/periode")
    public ResponseEntity<List<BonDeSortieMaterielDTO>> getByPeriode(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateDebut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFin) {
        return ResponseEntity.ok(bonSortieMaterielService.getBonsByPeriode(dateDebut, dateFin));
    }

    @PostMapping
    public ResponseEntity<BonDeSortieMaterielDTO> create(@Valid @RequestBody BonDeSortieMaterielDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bonSortieMaterielService.createBonSortie(dto));
    }

    @PostMapping("/demande/{demandeMaterielId}/initial")
    public ResponseEntity<BonDeSortieMaterielDTO> createInitial(
            @PathVariable String demandeMaterielId,
            @Valid @RequestBody BonDeSortieMaterielDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(bonSortieMaterielService.createInitialBon(demandeMaterielId, dto));
    }

    @PostMapping("/{bonInitialId}/complementaire")
    public ResponseEntity<BonDeSortieMaterielDTO> createComplementaire(
            @PathVariable String bonInitialId,
            @Valid @RequestBody BonDeSortieMaterielDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(bonSortieMaterielService.createBonComplementaire(bonInitialId, dto));
    }

    @PutMapping("/{id}/demande/{demandeId}")
    public ResponseEntity<BonDeSortieMaterielDTO> associateDemande(
            @PathVariable String id,
            @PathVariable String demandeId) {
        return ResponseEntity.ok(bonSortieMaterielService.associateDemandeToBon(id, demandeId));
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable String id) throws Exception {
        byte[] pdf = bonSortieMaterielService.generatePdf(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"bon-sortie-" + id + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/generate-numero")
    public ResponseEntity<String> generateNumeroBon() {
        return ResponseEntity.ok(bonSortieMaterielService.generateNumeroBon());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        bonSortieMaterielService.deleteBonSortie(id);
        return ResponseEntity.noContent().build();
    }
}
