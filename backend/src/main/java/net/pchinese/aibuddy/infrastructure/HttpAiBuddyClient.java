package net.pchinese.aibuddy.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.pchinese.common.error.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

@Component
public class HttpAiBuddyClient implements AiBuddyClient {
    private static final String PATH = "/internal/v1/ai-buddy/respond";
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final AiBuddyProperties properties; private final ObjectMapper objectMapper;
    public HttpAiBuddyClient(AiBuddyProperties properties, ObjectMapper objectMapper) { this.properties = properties; this.objectMapper = objectMapper; }
    public AiBuddyResponse respond(AiBuddyRequest request) {
        try {
            if (properties.getInternalHmacSecret() == null || properties.getInternalHmacSecret().isBlank()) throw unavailable();
            String body = objectMapper.writeValueAsString(new PrivateRequest("AI_BUDDY", request.context(), request.correlationId(), request.currentMessage(), request.requestId(), request.scenario().name()));
            String timestamp = Instant.now().toString(); String nonce = UUID.randomUUID().toString();
            String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(body.getBytes(StandardCharsets.UTF_8)));
            String signature = sign("POST\n" + PATH + "\n" + timestamp + "\n" + nonce + "\n" + hash);
            HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(properties.getInternalBaseUrl() + PATH)).timeout(Duration.ofSeconds(25))
                    .header("Content-Type", "application/json").header("X-Internal-Timestamp", timestamp).header("X-Internal-Nonce", nonce)
                    .header("X-Internal-Content-SHA256", hash).header("X-Internal-Signature", signature)
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8)).build();
            HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() == 200) return objectMapper.readValue(response.body(), AiBuddyResponse.class);
            if (response.statusCode() >= 500) throw unavailable();
            throw new ApiException(HttpStatus.BAD_GATEWAY, "PROVIDER_ERROR", "AI response could not be validated.");
        } catch (ApiException exception) { throw exception;
        } catch (Exception exception) { throw unavailable(); }
    }
    private String sign(String canonical) throws Exception { Mac mac = Mac.getInstance("HmacSHA256"); mac.init(new SecretKeySpec(properties.getInternalHmacSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256")); return HexFormat.of().formatHex(mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8))); }
    private ApiException unavailable() { return new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE", "AI service is temporarily unavailable."); }
    private record PrivateRequest(String capability, java.util.List<ContextMessage> context, UUID correlationId, String currentMessage, UUID requestId, String scenario) { }
}
