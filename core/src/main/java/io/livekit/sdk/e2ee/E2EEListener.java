package io.livekit.sdk.e2ee;

/**
 * Listener for E2EE events.
 */
public interface E2EEListener {

    /**
     * Called when E2EE is enabled or disabled.
     */
    default void onE2EEStateChanged(boolean enabled) {}

    /**
     * Called when a new key is set.
     */
    default void onKeySet(int keyIndex) {}

    /**
     * Called when the key is rotated.
     */
    default void onKeyRotated(KeyInfo newKey) {}

    /**
     * Called when a key is ratcheted for a participant.
     */
    default void onKeyRatcheted(String participantIdentity, int keyIndex) {}

    /**
     * Called when an encryption/decryption error occurs.
     */
    default void onEncryptionError(String participantIdentity, E2EEException error) {}
}
