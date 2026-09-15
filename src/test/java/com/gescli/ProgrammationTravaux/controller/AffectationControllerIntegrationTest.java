package com.gescli.ProgrammationTravaux.controller;

import com.gescli.ProgrammationTravaux.dto.AffectationResponseDTO;
import com.gescli.ProgrammationTravaux.service.AffectationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.ActiveProfiles;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AffectationController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class AffectationControllerIntegrationTest {
    @Autowired MockMvc mvc;
    @MockBean AffectationService service;

    @Test
    void forceFlagMustReachServiceContract() throws Exception {
        AffectationResponseDTO response = new AffectationResponseDTO();
        response.setId("aff-1"); response.setActive(true); response.setAgentId("a1"); response.setDevisId("d1");
        when(service.affecterAgent(argThat(req -> req.isForce() && "a1".equals(req.getAgentId()) && "d1".equals(req.getDevisId())))).thenReturn(response);

        mvc.perform(post("/api/affectations/affecter")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"agentId\":\"a1\",\"devisId\":\"d1\",\"force\":true}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value("aff-1"))
            .andExpect(jsonPath("$.active").value(true));
    }
}
