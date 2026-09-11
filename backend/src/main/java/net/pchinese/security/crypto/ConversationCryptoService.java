package net.pchinese.security.crypto;

import net.pchinese.aibuddy.persistence.AiConversationEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;

@Service
public class ConversationCryptoService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final UserDataKeyRepository userDataKeys;
    private final KmsEnvelopeService kms;
    public ConversationCryptoService(UserDataKeyRepository userDataKeys, KmsEnvelopeService kms) { this.userDataKeys = userDataKeys; this.kms = kms; }

    @Transactional
    public byte[] newWrappedConversationKey(UUID userId, Instant now) { return encrypt(randomBytes(32), userDek(userId, now)); }
    public byte[] encryptForConversation(AiConversationEntity conversation, byte[] value) { return encrypt(value, conversationDek(conversation)); }
    public byte[] decryptForConversation(AiConversationEntity conversation, byte[] value) { return decrypt(value, conversationDek(conversation)); }

    private byte[] conversationDek(AiConversationEntity conversation) {
        if (conversation.getWrappedConversationDek() == null) throw new IllegalStateException("Conversation key is unavailable.");
        return decrypt(conversation.getWrappedConversationDek(), userDek(conversation.getUserId(), Instant.now()));
    }
    private byte[] userDek(UUID userId, Instant now) {
        UserDataKeyEntity key = userDataKeys.findById(userId).orElseGet(() -> userDataKeys.save(
                UserDataKeyEntity.active(userId, kms.wrap(randomBytes(32)), kms.keyReference(), now)));
        if (!key.isActive()) throw new IllegalStateException("User data key is unavailable.");
        return kms.unwrap(key.getWrappedUserDek());
    }
    private static byte[] encrypt(byte[] value, byte[] key) {
        try { byte[] iv = randomBytes(12); Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, iv));
            byte[] encrypted = cipher.doFinal(value); byte[] output = Arrays.copyOf(iv, iv.length + encrypted.length);
            System.arraycopy(encrypted, 0, output, iv.length, encrypted.length); return output;
        } catch (GeneralSecurityException e) { throw new IllegalStateException("Unable to encrypt conversation data.", e); }
    }
    private static byte[] decrypt(byte[] value, byte[] key) {
        try { Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, Arrays.copyOfRange(value, 0, 12)));
            return cipher.doFinal(Arrays.copyOfRange(value, 12, value.length));
        } catch (GeneralSecurityException e) { throw new IllegalStateException("Unable to decrypt conversation data.", e); }
    }
    private static byte[] randomBytes(int size) { byte[] output = new byte[size]; RANDOM.nextBytes(output); return output; }
}
