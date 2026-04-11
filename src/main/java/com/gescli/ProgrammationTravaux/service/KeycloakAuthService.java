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

    public KeycloakAuthService(
            @Value("${keycloak.token-url}") String tokenUrl,
            @Value("${keycloak.client-id}") String clientId,
            @Value("${keycloak.client-secret}") String clientSecret) {
        this.tokenUrl = tokenUrl;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
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
            String baseUrl = tokenUrl.substring(0, tokenUrl.indexOf("/protocol/"));
            String realmName = baseUrl.substring(baseUrl.lastIndexOf("/") + 1);
            String serverUrl = baseUrl.substring(0, baseUrl.indexOf("/realms/"));
            String usersUrl = serverUrl + "/admin/realms/" + realmName + "/users";

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
                    .uri(usersUrl)
                    .header("Authorization", "Bearer " + adminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(user.toString())
                    .retrieve()
                    .toBodilessEntity();

            String location = response.getHeaders().getLocation().toString();
            String userId = location.substring(location.lastIndexOf("/") + 1);
            assignRoleToUser(userId, roleName, adminToken, serverUrl, realmName);
            log.info("Utilisateur Keycloak créé : {}", username);
            return userId;
        } catch (Exception e) {
            log.error("Erreur lors de la création de l'utilisateur Keycloak", e);
            throw new RuntimeException("Erreur création utilisateur Keycloak : " + e.getMessage(), e);
        }
    }

    private void assignRoleToUser(String userId, String roleName, String adminToken, String serverUrl, String realmName) {
        try {
            String roleUrl = serverUrl + "/admin/realms/" + realmName + "/roles/" + roleName;
            String roleBody = restClient.get()
                    .uri(roleUrl)
                    .header("Authorization", "Bearer " + adminToken)
                    .retrieve()
                    .body(String.class);
            JsonNode roleNode = mapper.readTree(roleBody);
            ArrayNode rolesArray = mapper.createArrayNode();
            rolesArray.add(roleNode);
            String mappingUrl = serverUrl + "/admin/realms/" + realmName + "/users/" + userId + "/role-mappings/realm";
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

    public void updateUser(String userId, String firstName, String lastName) {
        String adminToken = getAdminToken();
        String baseUrl = tokenUrl.substring(0, tokenUrl.indexOf("/protocol/"));
        String realmName = baseUrl.substring(baseUrl.lastIndexOf("/") + 1);
        String serverUrl = baseUrl.substring(0, baseUrl.indexOf("/realms/"));
        String userUrl = serverUrl + "/admin/realms/" + realmName + "/users/" + userId;

        ObjectNode body = mapper.createObjectNode();
        if (firstName != null) body.put("firstName", firstName);
        if (lastName  != null) body.put("lastName",  lastName);

        restClient.put()
                .uri(userUrl)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body.toString())
                .retrieve()
                .toBodilessEntity();
        log.info("Profil Keycloak mis à jour pour userId: {}", userId);
    }

    public void changePassword(String userId, String newPassword) {
        String adminToken = getAdminToken();
        String baseUrl = tokenUrl.substring(0, tokenUrl.indexOf("/protocol/"));
        String realmName = baseUrl.substring(baseUrl.lastIndexOf("/") + 1);
        String serverUrl = baseUrl.substring(0, baseUrl.indexOf("/realms/"));
        String resetUrl = serverUrl + "/admin/realms/" + realmName + "/users/" + userId + "/reset-password";

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
            String baseUrl = tokenUrl.substring(0, tokenUrl.indexOf("/protocol/"));
            String realmName = baseUrl.substring(baseUrl.lastIndexOf("/") + 1);
            String serverUrl = baseUrl.substring(0, baseUrl.indexOf("/realms/"));
            restClient.delete()
                    .uri(serverUrl + "/admin/realms/" + realmName + "/users/" + userId)
                    .header("Authorization", "Bearer " + adminToken)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Utilisateur Keycloak supprimé : {}", userId);
        } catch (Exception e) {
            log.warn("Impossible de supprimer l'utilisateur Keycloak {} : {}", userId, e.getMessage());
        }
    }
}