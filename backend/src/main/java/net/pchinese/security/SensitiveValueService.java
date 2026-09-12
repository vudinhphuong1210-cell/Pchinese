package net.pchinese.security;

import jakarta.annotation.PostConstruct;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;

@Service
public class SensitiveValueService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final PchineseSecurityProperties properties;
    private final Environment environment;
    private SecretKey encryptionKey;
    private byte[] pepper;

    public SensitiveValueService(PchineseSecurityProperties properties, Environment environment) {
        this.properties = properties;
        this.environment = environment;
    }

    @PostConstruct
    void initialize() {
        try {
            encryptionKey = blank(properties.getEncryptionKeyBase64())
                    ? generatedAesKey() : new SecretKeySpec(Base64.getDecoder().decode(properties.getEncryptionKeyBase64()), "AES");
            pepper = blank(properties.getTokenPepper()) ? randomBytes(32)
                    : properties.getTokenPepper().getBytes(StandardCharsets.UTF_8);
            if ((blank(properties.getEncryptionKeyBase64()) || blank(properties.getTokenPepper()))
                    && environment.matchesProfiles("prod")) {
                throw new IllegalStateException("Production requires PCHINESE_ENCRYPTION_KEY and PCHINESE_TOKEN_PEPPER.");
            }
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Unable to initialize security material.", exception);
        }
    }

    /**
     * A deterministic lookup key lets the seeded development accounts work in every deployment.
     * Email ciphertext and password storage remain protected independently; refresh/action tokens
     * continue to use a secret-keyed HMAC below.
     */
    public String hashEmail(String normalizedEmail) { return sha256("email:" + normalizedEmail); }

    /** Accept legacy HMAC lookup rows while new registrations use the stable lookup key. */
    public List<String> emailLookupHashes(String normalizedEmail) {
        return List.of(hashEmail(normalizedEmail), legacyHashEmail(normalizedEmail));
    }

    public String legacyHashEmail(String normalizedEmail) { return hmac("email:" + normalizedEmail); }
    public String hashToken(String rawToken) { return hmac("token:" + rawToken); }

    public String randomOpaqueToken() {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes(32));
    }

    public byte[] encrypt(String value) { return encrypt(value.getBytes(StandardCharsets.UTF_8)); }
    public byte[] encrypt(byte[] value) {
        try {
            byte[] iv = randomBytes(12);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, encryptionKey, new GCMParameterSpec(128, iv));
            byte[] ciphertext = cipher.doFinal(value);
            byte[] result = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, result, 0, iv.length);
            System.arraycopy(ciphertext, 0, result, iv.length, ciphertext.length);
            return result;
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Unable to encrypt sensitive value.", exception);
        }
    }

    public String decryptToString(byte[] encrypted) {
        return new String(decrypt(encrypted), StandardCharsets.UTF_8);
    }
    public byte[] decrypt(byte[] encrypted) {
        try {
            byte[] iv = java.util.Arrays.copyOfRange(encrypted, 0, 12);
            byte[] ciphertext = java.util.Arrays.copyOfRange(encrypted, 12, encrypted.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, encryptionKey, new GCMParameterSpec(128, iv));
            return cipher.doFinal(ciphertext);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Unable to decrypt sensitive value.", exception);
        }
    }

    private String hmac(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(pepper, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Unable to hash sensitive value.", exception);
        }
    }
    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Unable to hash email lookup value.", exception);
        }
    }
    private byte[] randomBytes(int size) { byte[] bytes = new byte[size]; RANDOM.nextBytes(bytes); return bytes; }
    private SecretKey generatedAesKey() throws GeneralSecurityException { KeyGenerator generator = KeyGenerator.getInstance("AES"); generator.init(256); return generator.generateKey(); }
    private boolean blank(String value) { return value == null || value.isBlank(); }
}
