package com.gescli.ProgrammationTravaux.config;

import com.gescli.ProgrammationTravaux.entity.Role;
import com.gescli.ProgrammationTravaux.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class RoleInitializer implements ApplicationRunner {

    private final RoleRepository roleRepository;

    @Override
    public void run(ApplicationArguments args) {
        List<String> roles = List.of("ADMIN", "CHEF", "AGENT");
        for (String libelle : roles) {
            if (!roleRepository.existsByLibelle(libelle)) {
                Role role = new Role();
                role.setLibelle(libelle);
                roleRepository.save(role);
            }
        }
    }
}
