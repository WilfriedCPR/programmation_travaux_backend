package com.gescli.ProgrammationTravaux.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gescli.ProgrammationTravaux.entity.Devis;
import com.gescli.ProgrammationTravaux.mapper.DemandeMaterielMapper;
import com.gescli.ProgrammationTravaux.mapper.DemandeMaterielMapperImpl;
import com.gescli.ProgrammationTravaux.repository.DemandeMaterielRepository;
import com.gescli.ProgrammationTravaux.repository.DevisRepository;
import com.gescli.ProgrammationTravaux.service.DemandeMaterielService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DataJpaTest
@ActiveProfiles("test")
@Import({DemandeMaterielService.class, DemandeMaterielMapperImpl.class})
class DemandeMaterielControllerIntegrationTest {
    @Autowired DemandeMaterielService service;
    @Autowired DemandeMaterielMapper mapper;
    @Autowired DemandeMaterielRepository demandes;
    @Autowired DevisRepository devisRepository;
    @Autowired EntityManager entityManager;

    @ParameterizedTest
    @ValueSource(strings = {"", ",\"dateDemande\":null", ",\"dateDemande\":\"2000-01-01T00:00:00\""})
    void persistedDateSurvivesCreationAllReadEndpointsAndUpdates(String updateDate) throws Exception {
        Devis devis = new Devis();
        devis.setId("d-date");
        devis.setDevisCode("DEV-DATE");
        devisRepository.saveAndFlush(devis);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new DemandeMaterielController(service, mapper)).build();
        String payload = "{\"devisId\":\"d-date\",\"referenceValue\":\"REF-DATE\",\"lignes\":[]";
        LocalDateTime beforeCreation = LocalDateTime.now().withNano(0);
        String response = mvc.perform(post("/api/demandes-materiel")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON).content(payload + "}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        var json = new ObjectMapper().readTree(response);
        String id = json.get("id").asText();
        String date = json.get("dateDemande").asText();
        assertThat(LocalDateTime.parse(date)).isBetween(beforeCreation, LocalDateTime.now());

        // Vider le contexte JPA oblige les GET à relire la valeur persistée.
        entityManager.flush();
        entityManager.clear();
        LocalDateTime persisted = demandes.findById(id).orElseThrow().getDateDemande();
        assertThat(persisted.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))).isEqualTo(date);
        assertReadEndpoints(mvc, id, date);

        mvc.perform(put("/api/demandes-materiel/{id}", id)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON).content(payload + updateDate + "}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.dateDemande").value(date));
        entityManager.flush();
        entityManager.clear();
        assertThat(demandes.findById(id).orElseThrow().getDateDemande()).isEqualTo(persisted);
        assertReadEndpoints(mvc, id, date);
    }

    private void assertReadEndpoints(MockMvc mvc, String id, String date) throws Exception {
        entityManager.clear();
        mvc.perform(get("/api/demandes-materiel/{id}", id).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andExpect(jsonPath("$.dateDemande").value(date));
        for (String path : new String[]{"", "/devis/d-date", "/devis/d-date/all"}) {
            entityManager.clear();
            mvc.perform(get("/api/demandes-materiel" + path).accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk()).andExpect(jsonPath("$[0].dateDemande").value(date));
        }
    }
}
