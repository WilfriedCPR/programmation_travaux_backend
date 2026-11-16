package com.gescli.ProgrammationTravaux.controller;

import com.gescli.ProgrammationTravaux.dto.AgentResponseDTO;
import com.gescli.ProgrammationTravaux.service.AgentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AgentController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class AgentControllerIntegrationTest {

    @Autowired MockMvc mvc;
    @MockBean AgentService agentService;

    @Test
    void lookupReturnsOperationalAgentList() throws Exception {
        AgentResponseDTO agent = new AgentResponseDTO();
        agent.setId("agent-1");
        agent.setNom("COMPAORE");
        agent.setPrenom("Jean");

        when(agentService.getAllAgents(any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(agent)));

        mvc.perform(get("/api/agents/lookup"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value("agent-1"))
            .andExpect(jsonPath("$[0].nom").value("COMPAORE"));
    }
}
