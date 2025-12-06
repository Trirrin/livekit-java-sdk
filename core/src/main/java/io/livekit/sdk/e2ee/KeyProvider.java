package io.livekit.sdk.e2ee;

/**
 * Interface for providing encryption keys for E2EE. Implementations handle key storage, derivation,
 * and rotation. Based on LiveKit Android SDK's KeyProvider interface.
 */
public interface KeyProvider {

  /**
   * Get the current encryption key for a participant.
   *
   * @param participantIdentity the participant's identity
   * @return the current key info, or null if not available
   */
  KeyInfo getKey(String participantIdentity);

  /**
   * Get a specific key by index for a participant.
   *
   * @param participantIdentity the participant's identity
   * @param keyIndex the key index
   * @return the key info, or null if not available
   */
  KeyInfo getKey(String participantIdentity, int keyIndex);

  /**
   * Set a new key for a participant.
   *
   * @param key the raw key bytes
   * @param keyIndex the key index
   * @param participantIdentity the participant's identity
   */
  void setKey(byte[] key, int keyIndex, String participantIdentity);

  /**
   * Rotate to a new key for the local participant. The new key should be distributed to other
   * participants via a secure channel.
   *
   * @return the new key info
   */
  KeyInfo rotateKey();

  /**
   * Get the shared key used for all participants (if using shared key mode).
   *
   * @return the shared key, or null if not using shared key mode
   */
  KeyInfo getSharedKey();

  /**
   * Set the shared key for all participants.
   *
   * @param key the raw key bytes
   * @param keyIndex the key index
   */
  void setSharedKey(byte[] key, int keyIndex);

  /**
   * Ratchet the shared key to derive a new key from the current one.
   *
   * @param keyIndex the key index to ratchet
   * @return the new derived key bytes
   */
  byte[] ratchetSharedKey(int keyIndex);

  /**
   * Ratchet a participant's key to derive a new key.
   *
   * @param participantIdentity the participant's identity
   * @param keyIndex the key index to ratchet
   * @return the new derived key bytes
   */
  byte[] ratchetKey(String participantIdentity, int keyIndex);

  /**
   * Export the current key in a format suitable for sharing.
   *
   * @param participantIdentity the participant's identity
   * @return the exported key bytes
   */
  byte[] exportKey(String participantIdentity);

  /**
   * Export the shared key.
   *
   * @param keyIndex the key index
   * @return the exported key bytes
   */
  byte[] exportSharedKey(int keyIndex);

  /**
   * Get the latest key index for a participant.
   *
   * @param participantIdentity the participant's identity
   * @return the latest key index
   */
  int getLatestKeyIndex(String participantIdentity);

  /**
   * Clear all keys for a participant.
   *
   * @param participantIdentity the participant's identity
   */
  void clearKeys(String participantIdentity);

  /** Clear all stored keys. */
  void clearAllKeys();

  /**
   * @return true if using shared key mode for all participants
   */
  boolean isSharedKeyMode();
}
