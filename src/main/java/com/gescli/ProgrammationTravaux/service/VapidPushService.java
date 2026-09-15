package com.gescli.ProgrammationTravaux.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gescli.ProgrammationTravaux.entity.PushSubscription;
import com.gescli.ProgrammationTravaux.repository.PushSubscriptionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.security.interfaces.ECPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
public class VapidPushService {
    private final PushSubscriptionRepository repository;
    private final String subject;
    private final Path keysFile;
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient http = HttpClient.newBuilder().build();
    private final PrivateKey privateKey;
    private final String publicKeyBase64Url;

    public VapidPushService(
            PushSubscriptionRepository repository,
            @Value("${app.vapid.subject:mailto:admin@sonabel.bf}") String subject,
            @Value("${app.vapid.keys-file:./.vapid-keys.properties}") String keysFile) {
        this.repository = repository;
        this.subject = subject;
        this.keysFile = Path.of(keysFile).toAbsolutePath().normalize();
        KeyMaterial material = loadOrCreateKeys();
        this.privateKey = material.privateKey();
        this.publicKeyBase64Url = material.publicKeyBase64Url();
    }

    public String getPublicKey() { return publicKeyBase64Url; }

    @Transactional
    public void subscribe(String agentId, String endpoint, String p256dh, String auth) {
        PushSubscription sub = repository.findByEndpoint(endpoint).orElseGet(PushSubscription::new);
        sub.setAgentId(agentId);
        sub.setEndpoint(endpoint);
        sub.setP256dh(p256dh);
        sub.setAuth(auth);
        sub.setActif(true);
        sub.setDerniereUtilisation(LocalDateTime.now());
        repository.save(sub);
    }

    @Transactional
    public void unsubscribe(String agentId, String endpoint) {
        repository.findByEndpoint(endpoint).ifPresent(sub -> {
            if (agentId.equals(sub.getAgentId())) {
                sub.setActif(false);
                sub.setDerniereUtilisation(LocalDateTime.now());
                repository.save(sub);
            }
        });
    }

    /**
     * Envoi Web Push sans payload : aucune dépendance de chiffrement n'est nécessaire.
     * Le service worker affiche ensuite une alerte générique et l'application récupère
     * le détail depuis /api/notifications.
     */
    public void notifyAgent(String agentId) {
        repository.findByAgentIdAndActifTrue(agentId).forEach(this::sendNoPayload);
    }

    private void sendNoPayload(PushSubscription sub) {
        try {
            URI endpoint = URI.create(sub.getEndpoint());
            String aud = endpoint.getScheme() + "://" + endpoint.getAuthority();
            String token = createVapidJwt(aud);
            HttpRequest request = HttpRequest.newBuilder(endpoint)
                    .header("TTL", "86400")
                    .header("Urgency", "normal")
                    .header("Authorization", "vapid t=" + token + ", k=" + publicKeyBase64Url)
                    .header("Crypto-Key", "p256ecdsa=" + publicKeyBase64Url)
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();
            http.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                    .thenAccept(response -> {
                        int status = response.statusCode();
                        if (status == 404 || status == 410) {
                            deactivateSubscription(sub.getId());
                        } else if (status < 200 || status >= 300) {
                            log.warn("Push refusé status={} endpoint={}", status, shortEndpoint(sub.getEndpoint()));
                        } else {
                            touchSubscription(sub.getId());
                        }
                    })
                    .exceptionally(ex -> {
                        log.warn("Push impossible vers {} : {}", shortEndpoint(sub.getEndpoint()), ex.getMessage());
                        return null;
                    });
        } catch (Exception e) {
            log.warn("Push impossible : {}", e.getMessage());
        }
    }

    @Transactional
    protected void deactivateSubscription(String id) {
        repository.findById(id).ifPresent(s -> { s.setActif(false); repository.save(s); });
    }

    @Transactional
    protected void touchSubscription(String id) {
        repository.findById(id).ifPresent(s -> { s.setDerniereUtilisation(LocalDateTime.now()); repository.save(s); });
    }

    private String createVapidJwt(String audience) throws Exception {
        long now = Instant.now().getEpochSecond();
        ObjectNode header = mapper.createObjectNode();
        header.put("typ", "JWT");
        header.put("alg", "ES256");
        ObjectNode claims = mapper.createObjectNode();
        claims.put("aud", audience);
        claims.put("exp", now + 12 * 60 * 60);
        claims.put("sub", subject);

        String encodedHeader = b64(header.toString().getBytes(StandardCharsets.UTF_8));
        String encodedClaims = b64(claims.toString().getBytes(StandardCharsets.UTF_8));
        String signingInput = encodedHeader + "." + encodedClaims;

        Signature signature = Signature.getInstance("SHA256withECDSA");
        signature.initSign(privateKey);
        signature.update(signingInput.getBytes(StandardCharsets.US_ASCII));
        byte[] der = signature.sign();
        return signingInput + "." + b64(derToJose(der, 64));
    }

    private KeyMaterial loadOrCreateKeys() {
        try {
            if (Files.exists(keysFile)) {
                Properties props = new Properties();
                try (InputStream in = Files.newInputStream(keysFile)) { props.load(in); }
                String pub = props.getProperty("publicKey");
                String priv = props.getProperty("privateKey");
                if (pub != null && priv != null) {
                    KeyFactory factory = KeyFactory.getInstance("EC");
                    PrivateKey key = factory.generatePrivate(new PKCS8EncodedKeySpec(Base64.getUrlDecoder().decode(priv)));
                    return new KeyMaterial(key, pub);
                }
            }

            KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
            generator.initialize(new java.security.spec.ECGenParameterSpec("secp256r1"));
            KeyPair pair = generator.generateKeyPair();
            String pub = encodePublic((ECPublicKey) pair.getPublic());
            String priv = b64(pair.getPrivate().getEncoded());

            if (keysFile.getParent() != null) Files.createDirectories(keysFile.getParent());
            Properties props = new Properties();
            props.setProperty("publicKey", pub);
            props.setProperty("privateKey", priv);
            try (OutputStream out = Files.newOutputStream(keysFile)) {
                props.store(out, "SONABEL ProgrammationTravaux VAPID keys - ne pas versionner");
            }
            log.info("Clés VAPID générées dans {}", keysFile);
            return new KeyMaterial(pair.getPrivate(), pub);
        } catch (Exception e) {
            throw new IllegalStateException("Impossible d'initialiser les clés VAPID", e);
        }
    }

    private static String encodePublic(ECPublicKey key) {
        byte[] x = fixed(key.getW().getAffineX(), 32);
        byte[] y = fixed(key.getW().getAffineY(), 32);
        byte[] raw = new byte[65];
        raw[0] = 0x04;
        System.arraycopy(x, 0, raw, 1, 32);
        System.arraycopy(y, 0, raw, 33, 32);
        return b64(raw);
    }

    private static byte[] fixed(BigInteger n, int size) {
        byte[] raw = n.toByteArray();
        byte[] out = new byte[size];
        int src = Math.max(0, raw.length - size);
        int len = Math.min(size, raw.length);
        System.arraycopy(raw, src, out, size - len, len);
        return out;
    }

    /** Convertit la signature ECDSA ASN.1/DER en format JOSE R||S. */
    private static byte[] derToJose(byte[] der, int outputLength) {
        if (der.length < 8 || der[0] != 0x30) throw new IllegalArgumentException("Signature DER invalide");
        int offset = 2;
        if ((der[1] & 0x80) != 0) offset = 2 + (der[1] & 0x7f);
        if (der[offset] != 0x02) throw new IllegalArgumentException("Signature DER invalide (R)");
        int rLen = der[offset + 1] & 0xff;
        int rStart = offset + 2;
        int sTag = rStart + rLen;
        if (der[sTag] != 0x02) throw new IllegalArgumentException("Signature DER invalide (S)");
        int sLen = der[sTag + 1] & 0xff;
        int sStart = sTag + 2;
        int partLen = outputLength / 2;
        byte[] out = new byte[outputLength];
        copyInteger(der, rStart, rLen, out, 0, partLen);
        copyInteger(der, sStart, sLen, out, partLen, partLen);
        return out;
    }

    private static void copyInteger(byte[] source, int start, int len, byte[] dest, int destStart, int size) {
        while (len > 0 && source[start] == 0) { start++; len--; }
        int copy = Math.min(len, size);
        System.arraycopy(source, start + len - copy, dest, destStart + size - copy, copy);
    }

    private static String b64(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String shortEndpoint(String endpoint) {
        return endpoint.length() <= 60 ? endpoint : endpoint.substring(0, 60) + "…";
    }

    private record KeyMaterial(PrivateKey privateKey, String publicKeyBase64Url) {}
}
