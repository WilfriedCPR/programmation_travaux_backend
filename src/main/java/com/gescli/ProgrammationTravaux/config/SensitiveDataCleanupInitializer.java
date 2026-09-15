package com.gescli.ProgrammationTravaux.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SensitiveDataCleanupInitializer implements ApplicationRunner {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            int cleaned = jdbcTemplate.update("UPDATE agent SET password = NULL WHERE password IS NOT NULL");
            if (cleaned > 0) log.info("{} ancien(s) mot(s) de passe local(aux) supprimé(s) de la table agent", cleaned);
        } catch (Exception e) {
            // Compatible avec une future migration qui supprimera totalement la colonne.
            log.debug("Aucun nettoyage de colonne password nécessaire : {}", e.getMessage());
        }
    }
}
