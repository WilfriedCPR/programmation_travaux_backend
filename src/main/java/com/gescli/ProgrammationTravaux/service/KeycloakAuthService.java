package com.gescli.ProgrammationTravaux.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gescli.ProgrammationTravaux.exception.ConflictException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

@Slf4j
@Service
public class KeycloakAuthService {

    private final RestClient restClient = RestClient.create();
    private final ObjectMapper mapper = new ObjectMapper();

    private final String tokenUrl;
    private final String clientId;
    private final String clientSecret;
    private final String adminBaseUrl;
    private final String realmName;

    public KeycloakAuthService(
            @Value("${keycloak.token-url}") String tokenUrl,
            @Value("${keycloak.client-id}") String clientId,
            @Value("${keycloak.client-secret}") String clientSecret,
            @Value("${keycloak.admin-base-url}") String adminBaseUrl,
            @Value("${keycloak.realm-name}") String realmName) {
        this.tokenUrl = tokenUrl;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.adminBaseUrl = adminBaseUrl;
        this.realmName = realmName;
    }

    private String usersUrl() { return adminBaseUrl + "/admin/realms/" + realmName + "/users"; }
    private String rolesUrl() { return adminBaseUrl + "/admin/realms/" + realmName + "/roles"; }
    private String userUrl(String userId) { return usersUrl() + "/" + userId; }

    public String resolveAuthUsername(String loginInput) {
        if (loginInput == null || loginInput.isBlank()) return loginInput;
        try {
            String adminToken = getAdminToken();
            String searchUrl = usersUrl() + "?search=" + java.net.URLEncoder.encode(loginInput, "UTF-8");
            String response = restClient.get()
                    .uri(searchUrl)
                    .header("Authorization", "Bearer " + adminToken)
                    .retrieve()
                    .body(String.class);

            JsonNode users = mapper.readTree(response);
            if (users.isArray() && users.size() > 0) {
                for (JsonNode user : users) {
                    String username = user.path("username").asText("");
                    String email = user.path("email").asText("");
                    String fn = user.path("firstName").asText("");
                    String ln = user.path("lastName").asText("");

                    if (loginInput.equalsIgnoreCase(username) || loginInput.equalsIgnoreCase(email)) {
                        return username;
                    }
                    if (loginInput.equalsIgnoreCase(fn + " " + ln) || loginInput.equalsIgnoreCase(ln + " " + fn)) {
                        return username;
                    }
                }
                if (users.size() == 1) {
                    return users.get(0).path("username").asText(loginInput);
                }
            }
        } catch (Exception e) {
            log.warn("Impossible de résoudre le nom d'utilisateur pour : {}", loginInput);
        }
        return loginInput;
    }

    public ResponseEntity<String> exchangeCredentials(String username, String password) {
        log.info("Échange de credentials pour l'utilisateur : {}", username);
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "password");
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("username", username);
        body.add("password", password);
        try {
            String responseBody = restClient.post()
                    .uri(tokenUrl)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body)
                    .retrieve()
                    .body(String.class);
            return ResponseEntity.ok(responseBody);
        } catch (HttpClientErrorException e) {
            log.error("Échec d'authentification pour {} : status={}", username, e.getStatusCode());
            throw e;
        }
    }

    public ResponseEntity<String> refreshToken(String refreshToken) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "refresh_token");
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("refresh_token", refreshToken);
        String responseBody = restClient.post()
                .uri(tokenUrl)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(body)
                .retrieve()
                .body(String.class);
        return ResponseEntity.ok(responseBody);
    }

    public boolean revokeToken(String token) {
        try {
            String revokeUrl = tokenUrl.replace("/token", "/revoke");
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("token", token);
            body.add("client_id", clientId);
            body.add("client_secret", clientSecret);
            restClient.post()
                    .uri(revokeUrl)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (Exception e) {
            log.warn("Échec de révocation du token : {}", e.getMessage());
            return false;
        }
    }

    private String getAdminToken() {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "client_credentials");
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        String response = restClient.post()
                .uri(tokenUrl)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(body)
                .retrieve()
                .body(String.class);
        try {
            return mapper.readTree(response).get("access_token").asText();
        } catch (Exception e) {
            throw new RuntimeException("Impossible d'obtenir le token admin Keycloak", e);
        }
    }

    public String createKeycloakUser(String username, String password, String firstName, String lastName, String roleName) {
        String userId = null;
        try {
            String adminToken = getAdminToken();
            if (findUserIdByUsername(username, adminToken) != null) {
                throw new ConflictException("Un compte Keycloak existe déjà pour le matricule \"" + username + "\".");
            }
            ObjectNode user = mapper.createObjectNode();
            user.put("username", username);
            user.put("enabled", true);
            user.put("firstName", firstName);
            user.put("lastName", lastName);
            user.put("emailVerified", true);
            user.put("email", username.toLowerCase() + "@sonabel.bf");
            user.putArray("requiredActions");
            ArrayNode credentials = user.putArray("credentials");
            ObjectNode cred = credentials.addObject();
            cred.put("type", "password");
            cred.put("value", password);
            cred.put("temporary", false);

            var response = restClient.post()
                    .uri(usersUrl())
                    .header("Authorization", "Bearer " + adminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(user.toString())
                    .retrieve()
                    .toBodilessEntity();
            if (response.getHeaders().getLocation() == null) {
                throw new IllegalStateException("Keycloak n'a pas retourné l'identifiant du nouvel utilisateur");
            }
            String location = response.getHeaders().getLocation().toString();
            userId = location.substring(location.lastIndexOf("/") + 1);
            ensureRealmRole(roleName, adminToken);
            assignRoleToUser(userId, roleName, adminToken);
            log.info("Utilisateur Keycloak créé : {}", username);
            return userId;
        } catch (Exception e) {
            if (userId != null) {
                try { deleteUserStrict(userId); }
                catch (Exception cleanup) { log.warn("Nettoyage Keycloak impossible pour {} : {}", userId, cleanup.getMessage()); }
            }
            if (e instanceof ConflictException conflict) throw conflict;
            if (e instanceof HttpClientErrorException.Conflict) {
                throw new ConflictException("Un compte Keycloak existe déjà pour le matricule \"" + username + "\".", e);
            }
            log.error("Erreur lors de la création de l'utilisateur Keycloak", e);
            throw new IllegalStateException("Création du compte Keycloak impossible pour " + username + ". Vérifiez la disponibilité de Keycloak et les droits du client d'administration.", e);
        }
    }

    private void ensureRealmRole(String roleName, String adminToken) {
        try {
            restClient.get().uri(rolesUrl() + "/" + roleName)
                    .header("Authorization", "Bearer " + adminToken).retrieve().toBodilessEntity();
        } catch (HttpClientErrorException.NotFound e) {
            ObjectNode role = mapper.createObjectNode();
            role.put("name", roleName);
            role.put("description", "Rôle applicatif Programmation Travaux");
            restClient.post().uri(rolesUrl()).header("Authorization", "Bearer " + adminToken)
                    .contentType(MediaType.APPLICATION_JSON).body(role.toString()).retrieve().toBodilessEntity();
        }
    }

    private void assignRoleToUser(String userId, String roleName, String adminToken) {
        try {
            ensureRealmRole(roleName, adminToken);
            String roleBody = restClient.get().uri(rolesUrl() + "/" + roleName)
                    .header("Authorization", "Bearer " + adminToken).retrieve().body(String.class);
            JsonNode roleNode = mapper.readTree(roleBody);
            ArrayNode rolesArray = mapper.createArrayNode();
            rolesArray.add(roleNode);
            restClient.post().uri(userUrl(userId) + "/role-mappings/realm")
                    .header("Authorization", "Bearer " + adminToken)
                    .contentType(MediaType.APPLICATION_JSON).body(rolesArray.toString())
                    .retrieve().toBodilessEntity();
            log.info("Rôle {} assigné à l'utilisateur {}", roleName, userId);
        } catch (Exception e) {
            throw new RuntimeException("Impossible d'assigner le rôle " + roleName + " à l'utilisateur " + userId, e);
        }
    }

    /**
     * Met à jour l'identité Keycloak. Pour les anciens agents dont l'identifiant local
     * n'est pas l'identifiant Keycloak, une recherche de secours par matricule est faite.
     *
     * @return l'identifiant Keycloak réellement utilisé
     */
    public String updateUserIdentity(String preferredUserId, String currentUsername, String username, String firstName, String lastName) {
        try {
            String adminToken = getAdminToken();
            String resolvedId = resolveExistingUserId(preferredUserId, currentUsername, adminToken);
            ObjectNode body = mapper.createObjectNode();
            body.put("username", username);
            body.put("firstName", firstName);
            body.put("lastName", lastName);
            body.put("email", username.toLowerCase() + "@sonabel.bf");
            restClient.put().uri(userUrl(resolvedId))
                    .header("Authorization", "Bearer " + adminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body.toString()).retrieve().toBodilessEntity();
            return resolvedId;
        } catch (ConflictException | IllegalStateException e) {
            throw e;
        } catch (HttpClientErrorException.Conflict e) {
            throw new ConflictException("Le matricule \"" + username + "\" existe déjà dans Keycloak.", e);
        } catch (Exception e) {
            throw new IllegalStateException("Impossible de synchroniser le compte Keycloak de " + currentUsername + ". Vérifiez la disponibilité de Keycloak et les droits du client d'administration.", e);
        }
    }

    public void replaceApplicationRole(String preferredUserId, String username, String roleName) {
        try {
            String adminToken = getAdminToken();
            String resolvedId = resolveExistingUserId(preferredUserId, username, adminToken);
            String mappingUrl = userUrl(resolvedId) + "/role-mappings/realm";
            String current = restClient.get().uri(mappingUrl).header("Authorization", "Bearer " + adminToken).retrieve().body(String.class);
            JsonNode arr = mapper.readTree(current);
            ArrayNode remove = mapper.createArrayNode();
            if (arr.isArray()) for (JsonNode r : arr) {
                String n = r.path("name").asText("").toUpperCase();
                if (n.equals("ADMIN") || n.equals("CHEF") || n.equals("AGENT") || n.contains("CHEF DE DIVISION")) remove.add(r);
            }
            if (!remove.isEmpty()) restClient.method(org.springframework.http.HttpMethod.DELETE).uri(mappingUrl)
                    .header("Authorization", "Bearer " + adminToken)
                    .contentType(MediaType.APPLICATION_JSON).body(remove.toString()).retrieve().toBodilessEntity();
            assignRoleToUser(resolvedId, roleName, adminToken);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Synchronisation du rôle Keycloak impossible pour " + username + ".", e);
        }
    }

    public void logoutUserSessions(String preferredUserId, String username) {
        try {
            String adminToken = getAdminToken();
            String resolvedId = resolveExistingUserId(preferredUserId, username, adminToken);
            restClient.post().uri(userUrl(resolvedId) + "/logout")
                    .header("Authorization", "Bearer " + adminToken)
                    .retrieve().toBodilessEntity();
        } catch (Exception e) {
            log.warn("Impossible d'invalider immédiatement les sessions Keycloak de {} : {}", username, e.getMessage());
        }
    }

    public void logoutUserSessions(String userId) {
        logoutUserSessions(userId, null);
    }


    public void updateUser(String userId, String firstName, String lastName, String email) {
        String adminToken = getAdminToken();

        ObjectNode body = mapper.createObjectNode();
        if (firstName != null) body.put("firstName", firstName);
        if (lastName  != null) body.put("lastName",  lastName);
        if (email != null) body.put("email", email);

        restClient.put()
                .uri(userUrl(userId))
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body.toString())
                .retrieve()
                .toBodilessEntity();
        log.info("Profil Keycloak mis à jour pour userId: {}", userId);
    }

    public void changePassword(String userId, String newPassword) {
        String adminToken = getAdminToken();
        String resetUrl = userUrl(userId) + "/reset-password";

        ObjectNode body = mapper.createObjectNode();
        body.put("type", "password");
        body.put("value", newPassword);
        body.put("temporary", false);

        restClient.put()
                .uri(resetUrl)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body.toString())
                .retrieve()
                .toBodilessEntity();
        log.info("Mot de passe modifié pour userId: {}", userId);
    }


    public void setUserEnabled(String preferredUserId, String username, boolean enabled) {
        try {
            String adminToken = getAdminToken();
            String resolvedId = resolveExistingUserId(preferredUserId, username, adminToken);
            ObjectNode body = mapper.createObjectNode();
            body.put("enabled", enabled);
            restClient.put().uri(userUrl(resolvedId))
                    .header("Authorization", "Bearer " + adminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body.toString()).retrieve().toBodilessEntity();
            if (!enabled) logoutUserSessions(resolvedId, username);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            String action = enabled ? "réactiver" : "désactiver";
            throw new IllegalStateException("Impossible de " + action + " le compte Keycloak de " + username + ". Vérifiez que le compte existe et que le client Keycloak possède les droits d'administration.", e);
        }
    }

    public void setUserEnabled(String userId, boolean enabled) {
        setUserEnabled(userId, null, enabled);
    }


    private String resolveExistingUserId(String preferredUserId, String username, String adminToken) {
        if (preferredUserId != null && !preferredUserId.isBlank()) {
            try {
                restClient.get().uri(userUrl(preferredUserId))
                        .header("Authorization", "Bearer " + adminToken)
                        .retrieve().toBodilessEntity();
                return preferredUserId;
            } catch (HttpClientErrorException.NotFound ignored) {
                // Anciennes données : l'id local peut être différent de l'id Keycloak.
            }
        }

        String byUsername = findUserIdByUsername(username, adminToken);
        if (byUsername != null) return byUsername;
        throw new IllegalStateException("Compte Keycloak introuvable pour le matricule " + (username == null ? preferredUserId : username) + ".");
    }

    private String findUserIdByUsername(String username, String adminToken) {
        if (username == null || username.isBlank()) return null;
        try {
            String encoded = java.net.URLEncoder.encode(username, java.nio.charset.StandardCharsets.UTF_8);
            String response = restClient.get()
                    .uri(usersUrl() + "?username=" + encoded + "&exact=true")
                    .header("Authorization", "Bearer " + adminToken)
                    .retrieve().body(String.class);
            JsonNode users = mapper.readTree(response);
            if (users.isArray()) {
                for (JsonNode user : users) {
                    if (username.equalsIgnoreCase(user.path("username").asText(""))) {
                        String id = user.path("id").asText(null);
                        if (id != null && !id.isBlank()) return id;
                    }
                }
            }
            return null;
        } catch (Exception e) {
            throw new IllegalStateException("Recherche du compte Keycloak impossible pour " + username + ".", e);
        }
    }

    public void deleteUserStrict(String userId) {
        String adminToken = getAdminToken();
        try {
            restClient.delete().uri(userUrl(userId))
                    .header("Authorization", "Bearer " + adminToken)
                    .retrieve().toBodilessEntity();
            log.info("Utilisateur Keycloak supprimé : {}", userId);
        } catch (HttpClientErrorException.NotFound ignored) {
            log.info("Utilisateur Keycloak déjà absent : {}", userId);
        }
    }

    public void deleteUser(String userId) {
        try { deleteUserStrict(userId); }
        catch (Exception e) { log.warn("Impossible de supprimer l'utilisateur Keycloak {} : {}", userId, e.getMessage()); }
    }
}