package com.gescli.ProgrammationTravaux.dto;

import com.gescli.ProgrammationTravaux.entity.Document;
import lombok.Data;
import java.time.LocalDate;

@Data
public class DocumentDTO {
    private String id;
    private String typeDocument;
    private String demandeMaterielId;
    private String numeroDocument;
    private String client;
    private String site;
    private String codeProjet;
    private String codeClient;
    private LocalDate dateDocument;

    public static DocumentDTO fromEntity(Document d) {
        if (d == null) return null;
        DocumentDTO dto = new DocumentDTO();
        dto.setId(d.getId());
        dto.setTypeDocument(d.getTypeDocument() != null ? d.getTypeDocument().name() : null);
        dto.setNumeroDocument(d.getNumeroDocument());
        dto.setClient(d.getClient());
        dto.setSite(d.getSite());
        dto.setCodeProjet(d.getCodeProjet());
        dto.setCodeClient(d.getCodeClient());
        dto.setDateDocument(d.getDateDocument());
        if (d.getDemandeMateriel() != null) dto.setDemandeMaterielId(d.getDemandeMateriel().getId());
        return dto;
    }
}