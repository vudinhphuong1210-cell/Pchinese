package net.pchinese.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "pchinese.security")
public class PchineseSecurityProperties {
    private String issuer;
    private String audience;
    private String keyId;
    private String privateKeyPkcs8Base64;
    private String publicKeyX509Base64;
    private String encryptionKeyBase64;
    private String tokenPepper;
    private List<String> allowedOrigins = List.of();
    private boolean cookieSecure = true;

    public String getIssuer() { return issuer; }
    public void setIssuer(String issuer) { this.issuer = issuer; }
    public String getAudience() { return audience; }
    public void setAudience(String audience) { this.audience = audience; }
    public String getKeyId() { return keyId; }
    public void setKeyId(String keyId) { this.keyId = keyId; }
    public String getPrivateKeyPkcs8Base64() { return privateKeyPkcs8Base64; }
    public void setPrivateKeyPkcs8Base64(String privateKeyPkcs8Base64) { this.privateKeyPkcs8Base64 = privateKeyPkcs8Base64; }
    public String getPublicKeyX509Base64() { return publicKeyX509Base64; }
    public void setPublicKeyX509Base64(String publicKeyX509Base64) { this.publicKeyX509Base64 = publicKeyX509Base64; }
    public String getEncryptionKeyBase64() { return encryptionKeyBase64; }
    public void setEncryptionKeyBase64(String encryptionKeyBase64) { this.encryptionKeyBase64 = encryptionKeyBase64; }
    public String getTokenPepper() { return tokenPepper; }
    public void setTokenPepper(String tokenPepper) { this.tokenPepper = tokenPepper; }
    public List<String> getAllowedOrigins() { return allowedOrigins; }
    public void setAllowedOrigins(List<String> allowedOrigins) { this.allowedOrigins = allowedOrigins == null ? List.of() : allowedOrigins; }
    public boolean isCookieSecure() { return cookieSecure; }
    public void setCookieSecure(boolean cookieSecure) { this.cookieSecure = cookieSecure; }
}
