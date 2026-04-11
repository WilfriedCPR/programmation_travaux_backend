package com.gescli.ProgrammationTravaux.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class DemandeMaterielDTO {
    private String id;
    private boolean valide;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dateValidation;
    private String devisId;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dateDemande;
    private String referenceId;
    private String referenceValue;
    private String referenceType;
    private String bonDeSortieId;
    private List<LigneDemandeDTO> lignes;
}