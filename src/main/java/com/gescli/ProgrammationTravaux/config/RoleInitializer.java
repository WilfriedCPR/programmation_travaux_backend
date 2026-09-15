package com.gescli.ProgrammationTravaux.config;

import com.gescli.ProgrammationTravaux.entity.Agent;
import com.gescli.ProgrammationTravaux.entity.Role;
import com.gescli.ProgrammationTravaux.repository.AgentRepository;
import com.gescli.ProgrammationTravaux.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class RoleInitializer implements ApplicationRunner {

    private static final List<String> CANONICAL_ROLES = List.of("ADMIN", "CHEF", "AGENT");

    private final RoleRepository roleRepository;
    private final AgentRepository agentRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Map<String, Role> canonical = new LinkedHashMap<>();
        for (String name : CANONICAL_ROLES) {
            Role role = roleRepository.findByLibelle(name).orElseGet(() -> {
                Role created = new Role();
                created.setLibelle(name);
                return roleRepository.save(created);
            });
            canonical.put(name, role);
        }

        // L'application utilise un seul rôle métier par agent. On normalise les
        // anciens libellés (ex. "Chef de division technique") vers ADMIN/CHEF/AGENT.
        for (Agent agent : agentRepository.findAll()) {
            String target = canonicalRole(agent);
            Role targetRole = canonical.get(target);
            if (agent.getRoles().size() != 1 || !agent.getRoles().contains(targetRole)) {
                agent.getRoles().clear();
                agent.addRole(targetRole);
                agentRepository.save(agent);
            }
        }

        agentRepository.flush();

        // Les anciens rôles devenus orphelins ne sont plus proposés par l'API.
        roleRepository.findAll().stream()
                .filter(role -> !CANONICAL_ROLES.contains(role.getLibelle()))
                .forEach(roleRepository::delete);
    }

    private String canonicalRole(Agent agent) {
        for (Role role : agent.getRoles()) {
            String value = role.getLibelle() == null ? "" : role.getLibelle().trim().toUpperCase();
            if (value.contains("ADMIN")) return "ADMIN";
        }
        for (Role role : agent.getRoles()) {
            String value = role.getLibelle() == null ? "" : role.getLibelle().trim().toUpperCase();
            if (value.contains("CHEF")) return "CHEF";
        }
        return "AGENT";
    }
}
