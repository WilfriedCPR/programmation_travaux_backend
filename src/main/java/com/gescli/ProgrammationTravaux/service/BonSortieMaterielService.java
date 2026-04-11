package com.gescli.ProgrammationTravaux.service;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gescli.ProgrammationTravaux.dto.BonDeSortieMaterielDTO;
import com.gescli.ProgrammationTravaux.dto.LigneCreateDTO;
import com.gescli.ProgrammationTravaux.entity.Agent;
import com.gescli.ProgrammationTravaux.entity.BonSortieMateriel;
import com.gescli.ProgrammationTravaux.entity.DemandeMateriel;
import com.gescli.ProgrammationTravaux.entity.LigneBonSortieMateriel;
import com.gescli.ProgrammationTravaux.entity.Materiel;
import com.gescli.ProgrammationTravaux.mapper.BonSortieMaterielMapper;
import com.gescli.ProgrammationTravaux.repository.AgentRepository;
import com.gescli.ProgrammationTravaux.repository.BonSortieMaterielRepository;
import com.gescli.ProgrammationTravaux.repository.DemandeMaterielRepository;
import com.gescli.ProgrammationTravaux.repository.MaterielRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BonSortieMaterielService {

    private final BonSortieMaterielRepository bonSortieMaterielRepository;
    private final DemandeMaterielRepository demandeMaterielRepository;
    private final MaterielRepository materielRepository;
    private final AgentRepository agentRepository;
    private final BonSortieMaterielMapper bonSortieMaterielMapper;

    @Transactional
    public BonDeSortieMaterielDTO createBonSortie(BonDeSortieMaterielDTO bonDTO) {
        // Auto-générer le numéro si absent
        if (bonDTO.getNumeroBon() == null || bonDTO.getNumeroBon().isBlank()) {
            bonDTO.setNumeroBon(generateNumeroBon());
        }
        if (bonSortieMaterielRepository.existsByNumeroBon(bonDTO.getNumeroBon())) {
            bonDTO.setNumeroBon(generateNumeroBon());
        }

        DemandeMateriel demandeMateriel = demandeMaterielRepository.findById(bonDTO.getDemandeMaterielId())
                .orElseThrow(() -> new EntityNotFoundException("Demande de matériel non trouvée"));

        Agent agentValidateur = null;
        if (bonDTO.getAgentValidateurId() != null) {
            agentValidateur = agentRepository.findById(bonDTO.getAgentValidateurId())
                    .orElseThrow(() -> new EntityNotFoundException("Agent validateur non trouvé"));
        }

        BonSortieMateriel bonInitial = null;
        if (bonDTO.getBonInitialId() != null) {
            bonInitial = bonSortieMaterielRepository.findById(bonDTO.getBonInitialId())
                    .orElseThrow(() -> new EntityNotFoundException("Bon initial non trouvé"));
            if (bonInitial.getBonInitial() != null) {
                throw new IllegalArgumentException("Le bon initial spécifié est un bon complémentaire et ne peut pas être utilisé comme bon initial");
            }
            if (bonDTO.getDemandeMaterielId() != null && bonInitial.getDemandeMateriel() != null
                    && !bonDTO.getDemandeMaterielId().equals(bonInitial.getDemandeMateriel().getId())) {
                throw new IllegalArgumentException("Le bon complémentaire doit être lié à la même demande que le bon initial");
            }
            bonDTO.setNumeroDevis(bonInitial.getNumeroDevis());
        } else {
            if (bonDTO.getDemandeMaterielId() == null) {
                throw new IllegalArgumentException("demandeMaterielId est requis pour créer un bon initial");
            }
            var existingInitial = bonSortieMaterielRepository
                .findFirstByDemandeMateriel_IdAndBonInitialIsNull(bonDTO.getDemandeMaterielId());
            if (existingInitial.isPresent()) {
                throw new IllegalStateException("Un bon de sortie a déjà été généré pour cette demande");
            }
        }

        if (bonDTO.getNumeroDevis() == null || bonDTO.getNumeroDevis().isBlank()) {
            if (demandeMateriel.getDevis() != null) {
                String code = demandeMateriel.getDevis().getDevisCode();
                bonDTO.setNumeroDevis(code != null && !code.isBlank() ? code : "N/A");
            } else {
                bonDTO.setNumeroDevis("N/A");
            }
        }

        BonSortieMateriel bonSortie = bonSortieMaterielMapper.toEntity(bonDTO);
        bonSortie.setDemandeMateriel(demandeMateriel);
        bonSortie.setAgentValidateur(agentValidateur);
        bonSortie.setBonInitial(bonInitial);
        bonSortie.setDateSortie(LocalDateTime.now());
        bonSortie.setNumeroDevis(bonDTO.getNumeroDevis());
        bonSortie.setClientCode(bonDTO.getCodeClient());
        bonSortie.setClientNom(bonDTO.getClientNomComplet());
        bonSortie.setVille(bonDTO.getVille());
        bonSortie.setMotifOperation(bonDTO.getMotifOperation());
        bonSortie.setReferenceCommande(bonDTO.getReferenceCommande());

        if (bonDTO.getLignes() != null && !bonDTO.getLignes().isEmpty()) {
            java.util.Set<String> seenMateriels = new java.util.HashSet<>();
            for (LigneCreateDTO ligneDTO : bonDTO.getLignes()) {
                if (ligneDTO.getMaterielId() == null) {
                    throw new IllegalArgumentException("Chaque ligne doit référencer un materielId");
                }
                if (!seenMateriels.add(ligneDTO.getMaterielId())) {
                    throw new IllegalArgumentException("Le même matériel ne peut pas être sélectionné plusieurs fois dans un bon de sortie");
                }
            }

            for (LigneCreateDTO ligneDTO : bonDTO.getLignes()) {
                if (ligneDTO.getQuantiteSortie() == null || ligneDTO.getQuantiteSortie() < 1) {
                    throw new IllegalArgumentException("La quantité doit être supérieure ou égale à 1 pour chaque ligne");
                }
                LigneBonSortieMateriel ligne = new LigneBonSortieMateriel();
                ligne.setQuantiteSortie(ligneDTO.getQuantiteSortie());
                ligne.setUniteMesure(ligneDTO.getUniteMesure());

                Materiel materiel = materielRepository.findById(ligneDTO.getMaterielId())
                        .orElseThrow(() -> new EntityNotFoundException("Matériel non trouvé : " + ligneDTO.getMaterielId()));
                ligne.setMateriel(materiel);

                bonSortie.addLigne(ligne);
            }
        }

        BonSortieMateriel savedBon = bonSortieMaterielRepository.save(bonSortie);
        return bonSortieMaterielMapper.toDto(savedBon);
    }

    @Transactional(readOnly = true)
    public BonsParDemande listByDemande(String demandeMaterielId) {
        var initialOpt = bonSortieMaterielRepository
            .findFirstByDemandeMateriel_IdAndBonInitialIsNull(demandeMaterielId);
        BonDeSortieMaterielDTO initial = initialOpt.map(bonSortieMaterielMapper::toDto).orElse(null);
        List<BonDeSortieMaterielDTO> complementaires = java.util.Collections.emptyList();
        if (initialOpt.isPresent()) {
            complementaires = bonSortieMaterielMapper.toDTOs(
                bonSortieMaterielRepository.findByBonInitial(initialOpt.get())
            );
        }
        return new BonsParDemande(initial, complementaires);
    }

    @Transactional
    public BonDeSortieMaterielDTO createInitialBon(String demandeMaterielId, BonDeSortieMaterielDTO bonDTO) {
        bonDTO.setBonInitialId(null);
        bonDTO.setDemandeMaterielId(demandeMaterielId);
        return createBonSortie(bonDTO);
    }

    @Transactional(readOnly = true)
    public List<BonDeSortieMaterielDTO> getAllBonsSortie() {
        return bonSortieMaterielMapper.toDTOs(bonSortieMaterielRepository.findAll());
    }

    @Transactional(readOnly = true)
    public List<BonDeSortieMaterielDTO> getBonsInitiaux() {
        return bonSortieMaterielMapper.toDTOs(bonSortieMaterielRepository.findBonsInitiaux());
    }

    @Transactional(readOnly = true)
    public BonDeSortieMaterielDTO getBonSortieById(String id) {
        BonSortieMateriel bon = bonSortieMaterielRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Bon de sortie non trouvé avec l'ID : " + id));
        return bonSortieMaterielMapper.toDto(bon);
    }

    @Transactional(readOnly = true)
    public BonDeSortieMaterielDTO getBonSortieByNumero(String numeroBon) {
        BonSortieMateriel bon = bonSortieMaterielRepository.findByNumeroBon(numeroBon)
                .orElseThrow(() -> new EntityNotFoundException("Bon de sortie non trouvé avec le numéro : " + numeroBon));
        return bonSortieMaterielMapper.toDto(bon);
    }

    @Transactional(readOnly = true)
    public BonDeSortieMaterielDTO getInitialByDemande(String demandeMaterielId) {
        BonSortieMateriel bon = bonSortieMaterielRepository
                .findFirstByDemandeMateriel_IdAndBonInitialIsNull(demandeMaterielId)
                .orElseThrow(() -> new EntityNotFoundException("Aucun bon initial trouvé pour la demande : " + demandeMaterielId));
        return bonSortieMaterielMapper.toDto(bon);
    }

    @Transactional(readOnly = true)
    public List<BonDeSortieMaterielDTO> getBonsComplementaires(String bonInitialId) {
        BonSortieMateriel bonInitial = bonSortieMaterielRepository.findById(bonInitialId)
                .orElseThrow(() -> new EntityNotFoundException("Bon initial non trouvé"));
        return bonSortieMaterielMapper.toDTOs(bonSortieMaterielRepository.findByBonInitial(bonInitial));
    }

    @Transactional(readOnly = true)
    public List<BonDeSortieMaterielDTO> getBonsByClient(String clientNom) {
        return bonSortieMaterielMapper.toDTOs(bonSortieMaterielRepository.findByClientNomContainingIgnoreCase(clientNom));
    }

    @Transactional(readOnly = true)
    public List<BonDeSortieMaterielDTO> getBonsByDevis(String numeroDevis) {
        return bonSortieMaterielMapper.toDTOs(bonSortieMaterielRepository.findByNumeroDevis(numeroDevis));
    }

    @Transactional(readOnly = true)
    public List<BonDeSortieMaterielDTO> getBonsByDevisId(String devisId) {
        return bonSortieMaterielMapper.toDTOs(bonSortieMaterielRepository.findByDemandeMateriel_Devis_Id(devisId));
    }

    @Transactional(readOnly = true)
    public List<BonDeSortieMaterielDTO> getBonsByPeriode(LocalDateTime dateDebut, LocalDateTime dateFin) {
        return bonSortieMaterielMapper.toDTOs(bonSortieMaterielRepository.findByDateSortieBetween(dateDebut, dateFin));
    }

    @Transactional
    public BonDeSortieMaterielDTO createBonComplementaire(String bonInitialId, BonDeSortieMaterielDTO bonComplementaireDTO) {
        BonSortieMateriel bonInitial = bonSortieMaterielRepository.findById(bonInitialId)
                .orElseThrow(() -> new EntityNotFoundException("Bon initial non trouvé"));

        String numeroComplementaire = generateNumeroBonComplementaire(bonInitial.getNumeroBon());
        bonComplementaireDTO.setNumeroBon(numeroComplementaire);
        bonComplementaireDTO.setBonInitialId(bonInitialId);
        if (bonInitial.getDemandeMateriel() == null) {
            throw new IllegalStateException("Le bon initial n'est pas associé à une demande");
        }
        bonComplementaireDTO.setDemandeMaterielId(bonInitial.getDemandeMateriel().getId());
        bonComplementaireDTO.setNumeroDevis(bonInitial.getNumeroDevis());
        bonComplementaireDTO.setCodeClient(bonInitial.getClientCode());
        bonComplementaireDTO.setClientNomComplet(bonInitial.getClientNom());
        bonComplementaireDTO.setVille(bonInitial.getVille());

        return createBonSortie(bonComplementaireDTO);
    }

    @Transactional
    public BonDeSortieMaterielDTO associateDemandeToBon(String bonId, String demandeId) {
        BonSortieMateriel bon = bonSortieMaterielRepository.findById(bonId)
            .orElseThrow(() -> new EntityNotFoundException("Bon de sortie non trouvé"));
        DemandeMateriel demande = demandeMaterielRepository.findById(demandeId)
            .orElseThrow(() -> new EntityNotFoundException("Demande de matériel non trouvée"));
        bon.setDemandeMateriel(demande);
        BonSortieMateriel saved = bonSortieMaterielRepository.save(bon);
        return bonSortieMaterielMapper.toDto(saved);
    }

    @Transactional
    public void deleteBonSortie(String id) {
        if (!bonSortieMaterielRepository.existsById(id)) {
            throw new EntityNotFoundException("Bon de sortie non trouvé avec l'ID : " + id);
        }
        bonSortieMaterielRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public byte[] generatePdf(String id) throws Exception {
        BonSortieMateriel bon = bonSortieMaterielRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Bon de sortie introuvable : " + id));

        ClassPathResource reportResource = new ClassPathResource("reports/BonSortieStyled_fixed.jrxml");
        InputStream reportStream = reportResource.getInputStream();
        JasperReport jasperReport = JasperCompileManager.compileReport(reportStream);

        DateTimeFormatter dtf     = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter dtfFull = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        ClassPathResource logoResource = new ClassPathResource("reports/logo.png");
        InputStream logoStream = logoResource.getInputStream();
        Map<String, Object> params = new HashMap<>();
        params.put("P_LOGO_PATH",          logoStream);
        params.put("P_COMPLEMENTAIRE",     bon.getBonInitial() != null);
        params.put("P_NUMERO_BON",         bon.getNumeroBon());
        params.put("P_CLIENT_NOM",         bon.getClientNom()          != null ? bon.getClientNom()          : "");
        params.put("P_CODE_CLIENT",        bon.getClientCode()         != null ? bon.getClientCode()         : "");
        params.put("P_REFERENCE_COMMANDE", bon.getReferenceCommande()  != null ? bon.getReferenceCommande()  : "");
        params.put("P_NUMERO_DEVIS",       bon.getNumeroDevis()        != null ? bon.getNumeroDevis()        : "");
        params.put("P_TITRE_SECTION",      bon.getMotifOperation()     != null ? bon.getMotifOperation()     : "");
        params.put("P_BON_COMMANDE",       bon.getReferenceCommande()  != null ? bon.getReferenceCommande()  : "");
        params.put("P_DATE_COMMANDE",      bon.getDateSortie()         != null ? bon.getDateSortie().format(dtf)  : "");
        params.put("P_DATE_GENERATION",    LocalDateTime.now().format(dtfFull));

        List<Map<String, ?>> lignesList = bon.getLignes().stream()
                .map(l -> {
                    Map<String, Object> row = new HashMap<>();
                    row.put("code",        l.getMateriel() != null ? l.getMateriel().getCode()    : "");
                    row.put("designation", l.getMateriel() != null
                            ? (l.getMateriel().getDesignation() != null ? l.getMateriel().getDesignation() : l.getMateriel().getLibelle())
                            : "");
                    row.put("quantite",    l.getQuantiteSortie());
                    row.put("unite",       l.getUniteMesure() != null ? l.getUniteMesure() : "");
                    return row;
                })
                .collect(Collectors.toList());

        JRMapCollectionDataSource dataSource = new JRMapCollectionDataSource(lignesList);
        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, params, dataSource);
        return JasperExportManager.exportReportToPdf(jasperPrint);
    }

    private String generateNumeroBonComplementaire(String numeroBonInitial) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return numeroBonInitial + "-COMP-" + timestamp;
    }

    public String generateNumeroBon() {
        String year = String.valueOf(LocalDateTime.now().getYear());
        String sequence = String.format("%06d", bonSortieMaterielRepository.countByDateSortieBetween(
                LocalDateTime.now().withDayOfYear(1).withHour(0).withMinute(0).withSecond(0),
                LocalDateTime.now()) + 1);
        return "DEM-" + year + "-" + sequence;
    }

    public static class BonsParDemande {
        private BonDeSortieMaterielDTO initial;
        private List<BonDeSortieMaterielDTO> complementaires;

        public BonsParDemande() {}

        public BonsParDemande(BonDeSortieMaterielDTO initial, List<BonDeSortieMaterielDTO> complementaires) {
            this.initial = initial;
            this.complementaires = complementaires;
        }

        public BonDeSortieMaterielDTO getInitial() { return initial; }
        public void setInitial(BonDeSortieMaterielDTO initial) { this.initial = initial; }
        public List<BonDeSortieMaterielDTO> getComplementaires() { return complementaires; }
        public void setComplementaires(List<BonDeSortieMaterielDTO> complementaires) { this.complementaires = complementaires; }
    }
}
