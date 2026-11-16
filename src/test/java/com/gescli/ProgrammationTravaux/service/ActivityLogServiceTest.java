package com.gescli.ProgrammationTravaux.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ActivityLogServiceTest {
    private final ActivityLogService service = new ActivityLogService(null);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void currentActorUsesCurrentFullNameWithoutTechnicalLoginInDisplay() {
        Jwt jwt = Jwt.withTokenValue("test-token")
                .headers(headers -> headers.putAll(Map.of("alg", "none")))
                .subject("keycloak-sub")
                .claim("preferred_username", "john")
                .claim("given_name", "Wilfried")
                .claim("family_name", "COMPAORE")
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

        assertThat(service.currentActor()).isEqualTo("Wilfried COMPAORE");
    }

    @Test
    void currentActorFallsBackToTechnicalLoginWhenNameClaimsAreMissing() {
        Jwt jwt = Jwt.withTokenValue("test-token")
                .headers(headers -> headers.putAll(Map.of("alg", "none")))
                .subject("keycloak-sub")
                .claim("preferred_username", "john")
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

        assertThat(service.currentActor()).isEqualTo("john");
    }

    @Test
    void currentActorFallsBackToAuthenticationNameWithoutJwt() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("service-account");
        SecurityContextHolder.getContext().setAuthentication(authentication);

        assertThat(service.currentActor()).isEqualTo("service-account");
    }
}
