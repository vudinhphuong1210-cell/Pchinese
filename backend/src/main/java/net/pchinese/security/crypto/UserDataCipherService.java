package net.pchinese.security.crypto;

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
public class UserDataCipherService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private final UserDataKeyRepository userDataKeys;
    private final KmsEnvelopeService kms;

    public UserDataCipherService(UserDataKeyRepository userDataKeys, KmsEnvelopeService kms) {
        this.userDataKeys = userDataKeys;
        this.kms = kms;
    }

    @Transactional
    public byte[] encryptForUser(UUID userId, byte[] plaintext) {
        if (plaintext == null || plaintext.length == 0) {
            return null;
        }
        byte[] dek = getUserDek(userId);
        return encrypt(plaintext, dek);
    }

    @Transactional
    public byte[] decryptForUser(UUID userId, byte[] ciphertext) {
        if (ciphertext == null || ciphertext.length == 0) {
            return null;
        }
        byte[] dek = getUserDek(userId);
        return decrypt(ciphertext, dek);
    }

    private byte[] getUserDek(UUID userId) {
        Instant now = Instant.now();
        UserDataKeyEntity key = userDataKeys.findById(userId).orElseGet(() ->
                userDataKeys.save(UserDataKeyEntity.active(userId, kms.wrap(randomBytes(32)), kms.keyReference(), now))
        );
        if (!key.isActive()) {
            throw new IllegalStateException("User data key is unavailable.");
        }
        return kms.unwrap(key.getWrappedUserDek());
    }

    private static byte[] encrypt(byte[] value, byte[] key) {
        try {
            byte[] iv = randomBytes(12);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, iv));
            byte[] encrypted = cipher.doFinal(value);
            byte[] output = Arrays.copyOf(iv, iv.length + encrypted.length);
            System.arraycopy(encrypted, 0, output, iv.length, encrypted.length);
            return output;
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Unable to encrypt user data.", e);
        }
    }

    private static byte[] decrypt(byte[] value, byte[] key) {
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, Arrays.copyOfRange(value, 0, 12)));
            return cipher.doFinal(Arrays.copyOfRange(value, 12, value.length));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Unable to decrypt user data.", e);
        }
    }

    private static byte[] randomBytes(int size) {
        byte[] output = new byte[size];
        RANDOM.nextBytes(output);
        return output;
    }
}
