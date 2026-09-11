package net.pchinese.security.crypto;

public interface KmsEnvelopeService {
    byte[] wrap(byte[] plaintextKey);
    byte[] unwrap(byte[] wrappedKey);
    String keyReference();
}
