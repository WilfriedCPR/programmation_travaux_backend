package com.gescli.ProgrammationTravaux.config;

import com.gescli.ProgrammationTravaux.entity.Structure;
import com.gescli.ProgrammationTravaux.repository.AgentRepository;
import com.gescli.ProgrammationTravaux.repository.StructureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class StructureInitializer implements ApplicationRunner {

    private final StructureRepository structureRepository;
    private final AgentRepository agentRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Structure sonabel = structureRepository.findFirstByLibelleIgnoreCase("SONABEL")
                .orElseGet(() -> {
                    Structure structure = new Structure();
                    structure.setLibelle("SONABEL");
                    return structureRepository.save(structure);
                });

        // Règle métier : tous les comptes Agent de l'application sont SONABEL.
        agentRepository.findAll().forEach(agent -> {
            if (agent.getStructure() == null
                    || !"SONABEL".equalsIgnoreCase(agent.getStructure().getLibelle())) {
                agent.setStructure(sonabel);
                agentRepository.save(agent);
            }
        });
    }
}
