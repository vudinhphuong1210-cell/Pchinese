package net.pchinese.aibuddy.api;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import net.pchinese.aibuddy.application.AiConversationService;
import net.pchinese.aibuddy.application.AiMessageService;
import net.pchinese.aibuddy.domain.AiBuddyScenario;
import net.pchinese.common.api.ApiEnvelope;
import net.pchinese.security.UserPrincipal;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@Validated
@RequestMapping("/api/v1/ai-conversations")
public class AiBuddyController {
    private final AiConversationService conversations;
    private final AiMessageService messages;
    public AiBuddyController(AiConversationService conversations, AiMessageService messages) { this.conversations = conversations; this.messages = messages; }

    @PostMapping
    public ResponseEntity<ApiEnvelope<AiConversationService.ConversationView>> create(@AuthenticationPrincipal UserPrincipal actor,
                                                                                         @Valid @RequestBody CreateConversationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiEnvelope.success(conversations.create(actor.userId(), request.scenario(), request.title())));
    }
    @GetMapping
    public ApiEnvelope<java.util.List<AiConversationService.ConversationView>> list(@AuthenticationPrincipal UserPrincipal actor,
                                                                                      @RequestParam(defaultValue = "0") int page,
                                                                                      @RequestParam(defaultValue = "20") int size) {
        if (page < 0 || size < 1 || size > 50) throw net.pchinese.common.error.ApiException.validation("Invalid page request.");
        var result = conversations.list(actor.userId(), PageRequest.of(page, size));
        return ApiEnvelope.success(result.getContent(), Map.of("page", page, "size", size));
    }
    @GetMapping("/{conversationId}")
    public ApiEnvelope<AiConversationService.ConversationDetailView> read(@AuthenticationPrincipal UserPrincipal actor, @PathVariable UUID conversationId) {
        return ApiEnvelope.success(conversations.read(actor.userId(), conversationId));
    }
    @PatchMapping("/{conversationId}")
    public ApiEnvelope<AiConversationService.ConversationView> rename(@AuthenticationPrincipal UserPrincipal actor, @PathVariable UUID conversationId,
                                                                        @Valid @RequestBody RenameConversationRequest request) {
        return ApiEnvelope.success(conversations.rename(actor.userId(), conversationId, request.title()));
    }
    @DeleteMapping("/{conversationId}")
    public ApiEnvelope<Map<String, Object>> delete(@AuthenticationPrincipal UserPrincipal actor, @PathVariable UUID conversationId) {
        conversations.delete(actor.userId(), conversationId); return ApiEnvelope.success(Map.of());
    }
    @PostMapping("/{conversationId}/messages")
    public ApiEnvelope<AiMessageService.MessagePairView> send(@AuthenticationPrincipal UserPrincipal actor, @PathVariable UUID conversationId,
                                                                @Valid @RequestBody CreateMessageRequest request) {
        return ApiEnvelope.success(messages.send(actor.userId(), conversationId, request.clientRequestId(), request.content()));
    }
    public record CreateConversationRequest(@NotNull AiBuddyScenario scenario, @Size(min = 1, max = 120) String title) {
        @JsonAnySetter public void rejectUnknownField(String key, Object value) { throw new IllegalArgumentException("Unsupported conversation field."); }
    }
    public record RenameConversationRequest(@NotBlank @Size(max = 120) String title) {
        @JsonAnySetter public void rejectUnknownField(String key, Object value) { throw new IllegalArgumentException("Unsupported conversation field."); }
    }
    public record CreateMessageRequest(@NotBlank @Size(max = 1000) String content, @NotNull UUID clientRequestId) {
        @JsonAnySetter public void rejectUnknownField(String key, Object value) { throw new IllegalArgumentException("Unsupported message field."); }
    }
}
