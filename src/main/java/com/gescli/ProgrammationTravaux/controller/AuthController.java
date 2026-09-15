package com.gescli.ProgrammationTravaux.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gescli.ProgrammationTravaux.service.KeycloakAuthService;
import com.gescli.ProgrammationTravaux.service.AgentService;
import org.springframework.beans.factory.annotation.Value;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final KeycloakAuthService keycloakAuthService;
    private final ObjectMapper mapper;
    private final AgentService agentService;
    @Value("${app.cookie-secure:false}") private boolean cookieSecure;

    @PostMapping("/keycloak-login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req) {
        log.info("Tentative de connexion : {}", req.username());
        try {
            String resolvedUsername = keycloakAuthService.resolveAuthUsername(req.username());
            ResponseEntity<String> resp = keycloakAuthService.exchangeCredentials(resolvedUsername, req.password());
            JsonNode json = mapper.readTree(resp.getBody());
            String accessToken  = json.path("access_token").asText(null);
            String refreshToken = json.path("refresh_token").asText(null);

            ResponseCookie accessCookie = buildCookie("KC_ACCESS", accessToken, 3600);
            ResponseCookie refreshCookie = buildCookie("KC_REFRESH", refreshToken, 7 * 24 * 3600);

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, accessCookie.toString(), refreshCookie.toString())
                    .body(Map.of("status", "ok"));
        } catch (Exception e) {
            log.warn("Échec de connexion pour {} : {}", req.username(), e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Identifiants incorrects"));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("authenticated", false));
        }
        List<String> roles = new ArrayList<>();
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess != null && realmAccess.get("roles") instanceof Collection<?> r) {
            r.forEach(role -> roles.add(role.toString()));
        }
        Map<String, Object> resourceAccess = jwt.getClaimAsMap("resource_access");
        if (resourceAccess != null) {
            resourceAccess.values().forEach(v -> {
                if (v instanceof Map<?, ?> m && m.get("roles") instanceof Collection<?> r) {
                    r.forEach(role -> roles.add(role.toString()));
                }
            });
        }
        Map<String, Object> userInfo = new LinkedHashMap<>();
        userInfo.put("username",            jwt.getSubject());
        userInfo.put("preferred_username",  jwt.getClaimAsString("preferred_username"));
        userInfo.put("roles",               roles);
        userInfo.put("email",               jwt.getClaimAsString("email"));
        userInfo.put("prenom",              jwt.getClaimAsString("given_name"));
        userInfo.put("nom",                 jwt.getClaimAsString("family_name"));
        userInfo.put("code",                jwt.getClaimAsString("code"));
        userInfo.put("structure",           jwt.getClaimAsString("structure"));
        agentService.findAgentById(jwt.getSubject()).ifPresent(agent -> {
            userInfo.put("code", agent.getCode());
            userInfo.put("structure", agent.getStructureLibelle());
        });
        return ResponseEntity.ok(userInfo);
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@CookieValue(name = "KC_REFRESH", required = false) String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "no_refresh_token"));
        }
        try {
            ResponseEntity<String> resp = keycloakAuthService.refreshToken(refreshToken);
            JsonNode json = mapper.readTree(resp.getBody());
            String accessToken  = json.path("access_token").asText(null);
            String newRefresh   = json.path("refresh_token").asText(null);
            ResponseCookie accessCookie  = buildCookie("KC_ACCESS",  accessToken, 3600);
            ResponseCookie refreshCookie = buildCookie("KC_REFRESH", newRefresh,  7 * 24 * 3600);
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, accessCookie.toString(), refreshCookie.toString())
                    .body(Map.of("status", "ok"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "refresh_failed"));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            @CookieValue(name = "KC_REFRESH", required = false) String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            keycloakAuthService.revokeToken(refreshToken);
        }
        ResponseCookie clearAccess  = buildCookie("KC_ACCESS",  "", 0);
        ResponseCookie clearRefresh = buildCookie("KC_REFRESH", "", 0);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, clearAccess.toString(), clearRefresh.toString())
                .body(Map.of("status", "ok"));
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateProfileRequest req) {
        if (jwt == null) return ResponseEntity.status(401).build();
        String userId = jwt.getSubject();
        try {
            keycloakAuthService.updateUser(userId, req.firstName(), req.lastName(), req.email());
            agentService.syncProfileFromKeycloak(userId, req.firstName(), req.lastName());
            return ResponseEntity.ok(Map.of("status", "ok"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/change-password")
    public ResponseEntity<?> changePassword(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ChangePasswordRequest req) {
        if (jwt == null) return ResponseEntity.status(401).build();
        String userId = jwt.getSubject();
        String username = jwt.getClaimAsString("preferred_username");
        try {
            keycloakAuthService.exchangeCredentials(username, req.currentPassword());
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", "Mot de passe actuel incorrect"));
        }
        try {
            keycloakAuthService.changePassword(userId, req.newPassword());
            return ResponseEntity.ok(Map.of("status", "ok"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    private ResponseCookie buildCookie(String name, String value, long maxAge) {
        return ResponseCookie.from(name, value != null ? value : "")
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .sameSite("Lax")
                .maxAge(maxAge)
                .build();
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {}
    public record UpdateProfileRequest(@NotBlank String firstName, @NotBlank String lastName, String email) {}
    public record ChangePasswordRequest(@NotBlank String currentPassword, @NotBlank @Size(min = 6) String newPassword) {}
}