package net.pchinese.learning.application;

import net.pchinese.common.error.ApiException;
import net.pchinese.security.PchineseSecurityProperties;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class PlaybackCapabilityService {

    private static final Duration CAPABILITY_LIFETIME = Duration.ofMinutes(15);
    private final byte[] hmacKey;

    public PlaybackCapabilityService(PchineseSecurityProperties securityProperties) {
        String secret = securityProperties.getTokenPepper() != null ? securityProperties.getTokenPepper() : "pchinese-playback-pepper-key";
        this.hmacKey = secret.getBytes(StandardCharsets.UTF_8);
    }

    public record CapabilityResult(String token, Instant expiresAt) {}

    public CapabilityResult issueCapability(UUID userId, UUID lessonId, UUID segmentId) {
        Instant expiresAt = Instant.now().plus(CAPABILITY_LIFETIME);
        String payload = userId + ":" + lessonId + ":" + (segmentId != null ? segmentId : "none") + ":" + expiresAt.toEpochMilli();
        String signature = sign(payload);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8))
                + "." + signature;
        return new CapabilityResult(token, expiresAt);
    }

    public void validateCapability(String token, UUID expectedUserId, UUID expectedLessonId, UUID expectedSegmentId) {
        if (token == null || token.isBlank()) {
            throw ApiException.validation("Playback capability token is missing.");
        }
        String[] parts = token.split("\\.");
        if (parts.length != 2) {
            throw ApiException.validation("Invalid playback capability token format.");
        }
        String payload;
        try {
            payload = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw ApiException.validation("Invalid playback capability encoding.");
        }
        String signature = parts[1];
        if (!sign(payload).equals(signature)) {
            throw ApiException.validation("Playback capability signature verification failed.");
        }

        String[] payloadParts = payload.split(":");
        if (payloadParts.length != 4) {
            throw ApiException.validation("Invalid playback capability payload.");
        }

        UUID tokenUserId = UUID.fromString(payloadParts[0]);
        UUID tokenLessonId = UUID.fromString(payloadParts[1]);
        String tokenSegmentIdStr = payloadParts[2];
        long expiresAtEpochMs = Long.parseLong(payloadParts[3]);

        if (Instant.now().toEpochMilli() > expiresAtEpochMs) {
            throw ApiException.validation("Playback capability token has expired.");
        }
        if (!tokenUserId.equals(expectedUserId) || !tokenLessonId.equals(expectedLessonId)) {
            throw ApiException.validation("Playback capability does not match the active session or lesson.");
        }
        if (expectedSegmentId != null && !"none".equals(tokenSegmentIdStr) && !tokenSegmentIdStr.equals(expectedSegmentId.toString())) {
            throw ApiException.validation("Playback capability does not match the active segment.");
        }
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(hmacKey, "HmacSHA256"));
            byte[] raw = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(raw);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to sign playback capability", e);
        }
    }
}
