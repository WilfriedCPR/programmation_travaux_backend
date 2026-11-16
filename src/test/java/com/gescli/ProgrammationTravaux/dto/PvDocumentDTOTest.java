package com.gescli.ProgrammationTravaux.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.gescli.ProgrammationTravaux.entity.Document;
import com.gescli.ProgrammationTravaux.entity.PvKind;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class PvDocumentDTOTest {
    @Test
    void exposesPersistedReferenceAndDateWithoutChangingDownloadMetadata() throws Exception {
        Document document = new Document();
        document.setId("pv-1");
        document.setNumeroDocument("PV-2026-001");
        document.setDateDocument(LocalDate.of(2026, 9, 17));
        document.setPvKind(PvKind.ETAPE);
        document.setFileName("etape.pdf");
        document.setFilePath("pv/etape.pdf");
        document.setMimeType("application/pdf");

        PvDocumentDTO dto = PvDocumentDTO.fromEntity(document);
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        var json = mapper.readTree(mapper.writeValueAsString(dto));
        assertThat(json.get("numeroDocument").asText()).isEqualTo("PV-2026-001");
        assertThat(json.get("dateDocument").asText()).isEqualTo("2026-09-17");
        assertThat(dto.getId()).isEqualTo("pv-1");
        assertThat(dto.getFileName()).isEqualTo("etape.pdf");
        assertThat(dto.getFilePath()).isEqualTo("pv/etape.pdf");
        assertThat(dto.getMimeType()).isEqualTo("application/pdf");
        assertThat(dto.getPvKind()).isEqualTo("ETAPE");
    }

    @Test
    void missingReferenceAndDateStayAbsent() {
        PvDocumentDTO dto = PvDocumentDTO.fromEntity(new Document());
        assertThat(dto.getNumeroDocument()).isNull();
        assertThat(dto.getDateDocument()).isNull();
    }
}
