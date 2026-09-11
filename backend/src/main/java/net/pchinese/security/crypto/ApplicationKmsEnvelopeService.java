package net.pchinese.security.crypto;

import net.pchinese.security.SensitiveValueService;
import org.springframework.stereotype.Service;

@Service
public class ApplicationKmsEnvelopeService implements KmsEnvelopeService {
    private final SensitiveValueService sensitiveValues;
    public ApplicationKmsEnvelopeService(SensitiveValueService sensitiveValues) { this.sensitiveValues = sensitiveValues; }
    public byte[] wrap(byte[] plaintextKey) { return sensitiveValues.encrypt(plaintextKey); }
    public byte[] unwrap(byte[] wrappedKey) { return sensitiveValues.decrypt(wrappedKey); }
    public String keyReference() { return "application-kms-envelope-v1"; }
}
