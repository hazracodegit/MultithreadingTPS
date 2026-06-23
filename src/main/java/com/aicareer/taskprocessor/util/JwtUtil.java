package com.aicareer.taskprocessor.util;

import com.aicareer.taskprocessor.entity.Role;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

@Component
public class JwtUtil {

    private final ObjectMapper objectMapper;
    private final String secret;
    private final long expirationSeconds;

    public JwtUtil(ObjectMapper objectMapper,
                   @Value("${app.jwt.secret}") String secret,
                   @Value("${app.jwt.expiration-seconds}") long expirationSeconds) {
        this.objectMapper = objectMapper;
        this.secret = secret;
        this.expirationSeconds = expirationSeconds;
    }

    public String generateToken(String username, Role role) {
        try {
            String header = encodeJson(Map.of("alg", "HS256", "typ", "JWT"));
            String payload = encodeJson(Map.of(
                    "sub", username,
                    "role", role.name(),
                    "exp", Instant.now().plusSeconds(expirationSeconds).getEpochSecond()
            ));
            return header + "." + payload + "." + sign(header + "." + payload);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to generate JWT", ex);
        }
    }

    public boolean isValid(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return false;
            }
            boolean signatureMatches = sign(parts[0] + "." + parts[1]).equals(parts[2]);
            return signatureMatches && expiresAt(token) > Instant.now().getEpochSecond();
        } catch (Exception ex) {
            return false;
        }
    }

    public String username(String token) {
        return claims(token).get("sub").toString();
    }

    public String role(String token) {
        return claims(token).get("role").toString();
    }

    private long expiresAt(String token) {
        return Long.parseLong(claims(token).get("exp").toString());
    }

    private Map<String, Object> claims(String token) {
        try {
            String payload = token.split("\\.")[1];
            byte[] json = Base64.getUrlDecoder().decode(payload);
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid token", ex);
        }
    }

    private String encodeJson(Map<String, Object> value) throws Exception {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(objectMapper.writeValueAsBytes(value));
    }

    private String sign(String value) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
    }
}
