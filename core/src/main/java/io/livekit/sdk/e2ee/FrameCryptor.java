package io.livekit.sdk.e2ee;

/**
 * Interface for encrypting and decrypting media frames.
 * Implementations handle the actual cryptographic operations on RTP payloads.
 */
public interface FrameCryptor {

    /**
     * Encrypt a frame payload.
     *
     * @param payload the plaintext payload
     * @param keyInfo the key to use for encryption
     * @return the encrypted payload with any necessary headers/trailers
     */
    byte[] encrypt(byte[] payload, KeyInfo keyInfo);

    /**
     * Decrypt a frame payload.
     *
     * @param payload the encrypted payload
     * @param keyInfo the key to use for decryption
     * @return the decrypted plaintext payload
     * @throws E2EEException if decryption fails
     */
    byte[] decrypt(byte[] payload, KeyInfo keyInfo) throws E2EEException;

    /**
     * Get the encryption type used by this cryptor.
     *
     * @return the encryption type
     */
    EncryptionType getEncryptionType();

    /**
     * Check if this cryptor is enabled.
     *
     * @return true if encryption/decryption is active
     */
    boolean isEnabled();

    /**
     * Enable or disable the cryptor.
     *
     * @param enabled true to enable, false to disable
     */
    void setEnabled(boolean enabled);
}
