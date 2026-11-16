package com.gescli.ProgrammationTravaux.config;

import jakarta.servlet.Filter;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.*;

@Slf4j
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${cors.allowed-origins:http://localhost:4200}")
    private String allowedOrigins;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> {
                CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
                repository.setCookiePath("/");
                CsrfTokenRequestAttributeHandler handler = new CsrfTokenRequestAttributeHandler();
                handler.setCsrfRequestAttributeName("_csrf");
                csrf.csrfTokenRepository(repository)
                    .csrfTokenRequestHandler(handler);
            })
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/keycloak-login", "/api/auth/logout", "/api/auth/refresh").permitAll()
                .requestMatchers("/api/auth/me").authenticated()
                .requestMatchers("/api/public/**").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/activity-log/**").hasRole("ADMIN")
                .requestMatchers("/api/notifications/**").authenticated()
                // Administration des agents : ADMIN uniquement.
                // Le CHEF conserve seulement un endpoint de consultation opérationnelle pour l'affectation.
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/agents/lookup").hasAnyRole("ADMIN", "CHEF")
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/agents/**").hasRole("ADMIN")
                .requestMatchers("/api/agents/**").hasRole("ADMIN")
                // Administration des participants : ADMIN uniquement.
                // La liste des externes reste consultable par le CHEF lors de la préparation d'un planning.
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/participants/externes").hasAnyRole("ADMIN", "CHEF")
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/participants/**").hasRole("ADMIN")
                .requestMatchers("/api/participants/**").hasRole("ADMIN")
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/type-travaux/**").hasAnyRole("ADMIN", "CHEF", "AGENT")
                .requestMatchers("/api/type-travaux/**").hasAnyRole("ADMIN", "CHEF")
                // L'historique par agent appartient à l'administration des agents.
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/affectations/byAgent/**").hasRole("ADMIN")
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/affectations/**").hasAnyRole("ADMIN", "CHEF", "AGENT")
                .requestMatchers("/api/affectations/**").hasAnyRole("ADMIN", "CHEF")
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/demandes-materiel/**").hasAnyRole("ADMIN", "CHEF", "AGENT")
                .requestMatchers("/api/demandes-materiel/**").hasAnyRole("ADMIN", "CHEF")
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/bons-sortie/**").hasAnyRole("ADMIN", "CHEF", "AGENT")
                .requestMatchers("/api/bons-sortie/**").hasAnyRole("ADMIN", "CHEF")
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/devis/**").hasAnyRole("ADMIN", "CHEF", "AGENT")
                .requestMatchers("/api/devis/**").hasAnyRole("ADMIN", "CHEF")
                .requestMatchers("/api/materiels/**").hasAnyRole("ADMIN", "CHEF")
                .requestMatchers("/api/structures/**").hasRole("ADMIN")
                .requestMatchers("/api/roles/**").hasRole("ADMIN")
                // Les documents et PV ne sont pas exposés aux agents tant qu'un contrôle
                // de périmètre métier par document n'est pas disponible dans le service.
                .requestMatchers("/api/documents/**").hasAnyRole("ADMIN", "CHEF")
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/plannings/*/pv-docs/**").hasAnyRole("ADMIN", "CHEF", "AGENT")
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/plannings/*/pv-docs/**").hasAnyRole("ADMIN", "CHEF", "AGENT")
                .requestMatchers(org.springframework.http.HttpMethod.DELETE, "/api/plannings/*/pv-docs/**").hasAnyRole("ADMIN", "CHEF")
                .requestMatchers("/api/planning-travaux/**").hasAnyRole("ADMIN", "CHEF", "AGENT")
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/contraintes/**").hasAnyRole("ADMIN", "CHEF", "AGENT")
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/contraintes/planning/*").hasAnyRole("ADMIN", "CHEF", "AGENT")
                .requestMatchers(org.springframework.http.HttpMethod.PUT, "/api/contraintes/planning/*/contrainte/*/lever").hasAnyRole("ADMIN", "CHEF", "AGENT")
                .requestMatchers("/api/contraintes/**").hasAnyRole("ADMIN", "CHEF")
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/travaux/**").hasAnyRole("ADMIN", "CHEF", "AGENT")
                .requestMatchers("/api/travaux/**").hasAnyRole("ADMIN", "CHEF")
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/plannings/**").hasAnyRole("ADMIN", "CHEF", "AGENT")
                // CU06 : les trois profils peuvent créer un planning, dans le périmètre
                // du dossier affecté pour un AGENT.
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/plannings").hasAnyRole("ADMIN", "CHEF", "AGENT")
                // CU12 : renseigner la fin d'intervention et clôturer un planning.
                .requestMatchers(org.springframework.http.HttpMethod.PATCH, "/api/plannings/*/finish").hasAnyRole("ADMIN", "CHEF", "AGENT")
                .requestMatchers(org.springframework.http.HttpMethod.PATCH, "/api/plannings/*/close").hasAnyRole("ADMIN", "CHEF", "AGENT")
                .requestMatchers(org.springframework.http.HttpMethod.PUT, "/api/plannings/*/cloture").hasAnyRole("ADMIN", "CHEF", "AGENT")
                .requestMatchers(org.springframework.http.HttpMethod.PUT, "/api/plannings/*").hasAnyRole("ADMIN", "CHEF", "AGENT")
                .requestMatchers("/api/plannings/**").hasAnyRole("ADMIN", "CHEF")
                .requestMatchers("/api/auth/profile", "/api/auth/change-password").authenticated()
                .requestMatchers("/api/stats/**").authenticated()
                .anyRequest().authenticated()
            )
            .addFilterAfter(csrfCookieFilter(), CsrfFilter.class)
            .addFilterBefore(cookieToAuthHeaderFilter(), AbstractPreAuthenticatedProcessingFilter.class)
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(Arrays.asList(allowedOrigins.split(",")));
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(Collections.singletonList("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Set<GrantedAuthority> authorities = new HashSet<>();
            Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
            if (realmAccess != null && realmAccess.get("roles") instanceof Collection<?> roles) {
                roles.forEach(r -> addAuthoritiesForRole(authorities, r.toString()));
            }
            Map<String, Object> resourceAccess = jwt.getClaimAsMap("resource_access");
            if (resourceAccess != null) {
                resourceAccess.values().forEach(v -> {
                    if (v instanceof Map<?, ?> clientMap && clientMap.get("roles") instanceof Collection<?> roles) {
                        roles.forEach(r -> addAuthoritiesForRole(authorities, r.toString()));
                    }
                });
            }
            JwtGrantedAuthoritiesConverter scopeConverter = new JwtGrantedAuthoritiesConverter();
            authorities.addAll(scopeConverter.convert(jwt));
            return authorities;
        });
        return converter;
    }

    private void addAuthoritiesForRole(Collection<GrantedAuthority> authorities, String roleName) {
        authorities.add(new SimpleGrantedAuthority("ROLE_" + roleName));
        String lower = roleName.toLowerCase();
        if (lower.contains("admin")) authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        if (lower.contains("chef"))  authorities.add(new SimpleGrantedAuthority("ROLE_CHEF"));
        if (lower.contains("agent")) authorities.add(new SimpleGrantedAuthority("ROLE_AGENT"));
    }

    private Filter csrfCookieFilter() {
        return (request, response, chain) -> {
            Object attribute = ((HttpServletRequest) request).getAttribute(CsrfToken.class.getName());
            if (attribute instanceof CsrfToken token) {
                token.getToken();
            }
            chain.doFilter(request, response);
        };
    }

    private Filter cookieToAuthHeaderFilter() {
        return (request, response, chain) -> {
            HttpServletRequest req = (HttpServletRequest) request;
            String uri = req.getRequestURI();
            if ("/api/auth/keycloak-login".equals(uri) || "/api/auth/logout".equals(uri) || "/api/auth/refresh".equals(uri)) {
                chain.doFilter(request, response);
                return;
            }
            if (req.getCookies() != null) {
                for (Cookie c : req.getCookies()) {
                    if ("KC_ACCESS".equals(c.getName()) && c.getValue() != null && !c.getValue().isBlank()) {
                        HttpServletRequest wrapped = new HttpServletRequestWrapper(req) {
                            @Override
                            public String getHeader(String name) {
                                if ("Authorization".equalsIgnoreCase(name)) return "Bearer " + c.getValue();
                                return super.getHeader(name);
                            }
                        };
                        chain.doFilter(wrapped, response);
                        return;
                    }
                }
            }
            chain.doFilter(request, response);
        };
    }
}
