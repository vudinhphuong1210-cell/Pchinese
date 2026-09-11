package net.pchinese.vocabulary.application;

import net.pchinese.security.crypto.UserDataCipherService;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Service
public class PersonalNoteCipherService {

    private static final int MAX_NOTE_LENGTH = 500;
    private final UserDataCipherService userCipherService;

    public PersonalNoteCipherService(UserDataCipherService userCipherService) {
        this.userCipherService = userCipherService;
    }

    public byte[] encryptNote(UUID userId, String notePlaintext) {
        if (notePlaintext == null) {
            return null;
        }
        validateNoteContent(notePlaintext);
        return userCipherService.encryptForUser(userId, notePlaintext.getBytes(StandardCharsets.UTF_8));
    }

    public String decryptNote(UUID userId, byte[] ciphertext) {
        if (ciphertext == null || ciphertext.length == 0) {
            return null;
        }
        byte[] decrypted = userCipherService.decryptForUser(userId, ciphertext);
        return decrypted != null ? new String(decrypted, StandardCharsets.UTF_8) : null;
    }

    public void validateNoteContent(String notePlaintext) {
        if (notePlaintext == null) {
            return;
        }
        if (notePlaintext.codePointCount(0, notePlaintext.length()) > MAX_NOTE_LENGTH) {
            throw new IllegalArgumentException("Personal note exceeds maximum allowed length of " + MAX_NOTE_LENGTH + " characters.");
        }
        // HTML / markdown / formatting detection heuristic
        if (containsFormattingOrAttachments(notePlaintext)) {
            throw new IllegalArgumentException("Personal note must be plain text without formatting or attachments.");
        }
    }

    private boolean containsFormattingOrAttachments(String text) {
        // Disallow HTML tags, markdown links, or binary attachment references
        return text.contains("<") && text.contains(">") || text.contains("![") || text.contains("data:image/") || text.contains("data:application/");
    }
}
