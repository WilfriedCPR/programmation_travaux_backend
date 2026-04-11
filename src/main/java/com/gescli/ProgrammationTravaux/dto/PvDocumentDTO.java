package com.gescli.ProgrammationTravaux.dto;

import com.gescli.ProgrammationTravaux.entity.Document;
import lombok.Data;

import java.util.Arrays;
import java.util.List;

@Data
public class PvDocumentDTO {
    private String id;
    private String fileName;
    private String filePath;
    private String mimeType;
    private String observation;
    private String pvKind;
    private List<String> participants;

    public static PvDocumentDTO fromEntity(Document d) {
        PvDocumentDTO dto = new PvDocumentDTO();
        dto.setId(d.getId());
        dto.setFileName(d.getFileName());
        dto.setFilePath(d.getFilePath());
        dto.setMimeType(d.getMimeType());
        dto.setObservation(d.getObservation());
        dto.setPvKind(d.getPvKind() != null ? d.getPvKind().name() : null);
        if (d.getParticipantsJson() != null && !d.getParticipantsJson().isBlank()) {
            try {
                String cleaned = d.getParticipantsJson().replaceAll("[\\[\\]\"\\s]", "");
                dto.setParticipants(Arrays.asList(cleaned.split(",")));
            } catch (Exception e) {
                dto.setParticipants(List.of());
            }
        } else {
            dto.setParticipants(List.of());
        }
        return dto;
    }
}
