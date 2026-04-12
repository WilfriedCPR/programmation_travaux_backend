package com.gescli.ProgrammationTravaux.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
        try {
            String adminToken = getAdminToken();

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

            String location = response.getHeaders().getLocation().toString();
            String userId = location.substring(location.lastIndexOf("/") + 1);
            assignRoleToUser(userId, roleName, adminToken);
            log.info("Utilisateur Keycloak créé : {}", username);
            return userId;
        } catch (Exception e) {
            log.error("Erreur lors de la création de l'utilisateur Keycloak", e);
            throw new RuntimeException("Erreur création utilisateur Keycloak : " + e.getMessage(), e);
        }
    }

    private void assignRoleToUser(String userId, String roleName, String adminToken) {
        try {
            String roleUrl = rolesUrl() + "/" + roleName;
            String roleBody = restClient.get()
                    .uri(roleUrl)
                    .header("Authorization", "Bearer " + adminToken)
                    .retrieve()
                    .body(String.class);
            JsonNode roleNode = mapper.readTree(roleBody);
            ArrayNode rolesArray = mapper.createArrayNode();
            rolesArray.add(roleNode);
            String mappingUrl = userUrl(userId) + "/role-mappings/realm";
            restClient.post()
                    .uri(mappingUrl)
                    .header("Authorization", "Bearer " + adminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(rolesArray.toString())
                    .retrieve()
                    .toBodilessEntity();
            log.info("Rôle {} assigné à l'utilisateur {}", roleName, userId);
        } catch (Exception e) {
            log.warn("Impossible d'assigner le rôle {} à l'utilisateur {} : {}", roleName, userId, e.getMessage());
        }
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

    public void deleteUser(String userId) {
        try {
            String adminToken = getAdminToken();
            restClient.delete()
                    .uri(userUrl(userId))
                    .header("Authorization", "Bearer " + adminToken)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Utilisateur Keycloak supprimé : {}", userId);
        } catch (Exception e) {
            log.warn("Impossible de supprimer l'utilisateur Keycloak {} : {}", userId, e.getMessage());
        }
    }
}