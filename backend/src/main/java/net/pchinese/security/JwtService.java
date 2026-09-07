package net.pchinese.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {
    private static final Duration ACCESS_TTL = Duration.ofMinutes(10);
    private final PchineseSecurityProperties properties;
    private final RSAPrivateKey privateKey;
    private final RSAPublicKey publicKey;
    private final Clock clock = Clock.systemUTC();

    public JwtService(PchineseSecurityProperties properties, Environment environment) {
        this.properties = properties;
        try {
            if (blank(properties.getPrivateKeyPkcs8Base64()) || blank(properties.getPublicKeyX509Base64())) {
                if (environment.matchesProfiles("prod")) {
                    throw new IllegalStateException("Production requires an RSA JWT key pair.");
                }
                KeyPair pair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
                privateKey = (RSAPrivateKey) pair.getPrivate();
                publicKey = (RSAPublicKey) pair.getPublic();
            } else {
                KeyFactory factory = KeyFactory.getInstance("RSA");
                privateKey = (RSAPrivateKey) factory.generatePrivate(new PKCS8EncodedKeySpec(
                        Base64.getDecoder().decode(properties.getPrivateKeyPkcs8Base64())));
                publicKey = (RSAPublicKey) factory.generatePublic(new X509EncodedKeySpec(
                        Base64.getDecoder().decode(properties.getPublicKeyX509Base64())));
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to initialize JWT signing keys.", exception);
        }
    }

    public AccessToken issue(UUID userId, UUID sessionId, long authzVersion) {
        Instant now = clock.instant();
        Instant expiresAt = now.plus(ACCESS_TTL);
        try {
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .issuer(properties.getIssuer())
                    .audience(properties.getAudience())
                    .subject(userId.toString())
                    .claim("sid", sessionId.toString())
                    .jwtID(UUID.randomUUID().toString())
                    .issueTime(Date.from(now))
                    .notBeforeTime(Date.from(now))
                    .expirationTime(Date.from(expiresAt))
                    .claim("typ", "access")
                    .claim("authzVersion", authzVersion)
                    .build();
            SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256)
                    .keyID(properties.getKeyId()).type(JOSEObjectType.JWT).build(), claims);
            jwt.sign(new RSASSASigner(privateKey));
            return new AccessToken(jwt.serialize(), expiresAt);
        } catch (JOSEException exception) {
            throw new IllegalStateException("Unable to issue access token.", exception);
        }
    }

    public JwtClaims validate(String token) throws JwtValidationException {
        try {
            SignedJWT jwt = SignedJWT.parse(token);
            if (!JWSAlgorithm.RS256.equals(jwt.getHeader().getAlgorithm())
                    || !properties.getKeyId().equals(jwt.getHeader().getKeyID())
                    || !jwt.verify(new RSASSAVerifier(publicKey))) {
                throw new JwtValidationException("Invalid access token.", false);
            }
            JWTClaimsSet claims = jwt.getJWTClaimsSet();
            Instant now = clock.instant();
            if (!properties.getIssuer().equals(claims.getIssuer())
                    || !claims.getAudience().contains(properties.getAudience())
                    || !"access".equals(claims.getStringClaim("typ"))
                    || claims.getExpirationTime() == null || !claims.getExpirationTime().toInstant().isAfter(now)
                    || claims.getNotBeforeTime() == null || claims.getNotBeforeTime().toInstant().isAfter(now)) {
                boolean expired = claims.getExpirationTime() != null && !claims.getExpirationTime().toInstant().isAfter(now);
                throw new JwtValidationException("Invalid access token.", expired);
            }
            return new JwtClaims(UUID.fromString(claims.getSubject()), UUID.fromString(claims.getStringClaim("sid")),
                    claims.getLongClaim("authzVersion"));
        } catch (JwtValidationException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new JwtValidationException("Invalid access token.", false);
        }
    }

    public record AccessToken(String value, Instant expiresAt) { }
    public record JwtClaims(UUID userId, UUID sessionId, long authzVersion) { }
    public static class JwtValidationException extends RuntimeException {
        private final boolean expired;
        public JwtValidationException(String message, boolean expired) { super(message); this.expired = expired; }
        public boolean expired() { return expired; }
    }
    private boolean blank(String value) { return value == null || value.isBlank(); }
}
