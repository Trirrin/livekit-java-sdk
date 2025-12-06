package io.livekit.sdk.e2ee;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages E2EE state and coordinates encryption/decryption for a room. Based on LiveKit Android
 * SDK's E2EEManager.
 *
 * <p>Note: Media track E2EE requires WebRTC FrameCryptor support which is not available in
 * webrtc-java. This manager currently supports DataChannel E2EE only.
 */
public class E2EEManager {
  private final E2EEOptions options;
  private final List<E2EEListener> listeners = new CopyOnWriteArrayList<>();
  private final DataPacketCryptor dataPacketCryptor;
  private boolean enabled;
  private boolean dataChannelEncryptionEnabled;
  private String localIdentity;

  public E2EEManager(E2EEOptions options) {
    this.options = options;
    this.enabled = true;
    this.dataChannelEncryptionEnabled = false;
    this.dataPacketCryptor = new DataPacketCryptor(options.getKeyProvider());
  }

  public E2EEOptions getOptions() {
    return options;
  }

  public KeyProvider getKeyProvider() {
    return options.getKeyProvider();
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
    this.dataPacketCryptor.setEnabled(enabled);
    notifyStateChanged(enabled);
  }

  /**
   * @return true if data channel encryption is enabled for outgoing messages
   */
  public boolean isDataChannelEncryptionEnabled() {
    return enabled && dataChannelEncryptionEnabled;
  }

  /** Enable or disable data channel encryption for outgoing messages. */
  public void setDataChannelEncryptionEnabled(boolean enabled) {
    this.dataChannelEncryptionEnabled = enabled;
  }

  /** Set the local participant's identity for key management. */
  public void setLocalIdentity(String identity) {
    this.localIdentity = identity;
    KeyProvider keyProvider = options.getKeyProvider();
    if (keyProvider instanceof BaseKeyProvider) {
      ((BaseKeyProvider) keyProvider).setLocalIdentity(identity);
    }
  }

  /**
   * Set the shared encryption key for all participants.
   *
   * @param key the raw key bytes (should be 32 bytes for AES-256)
   * @param keyIndex the key index (for key rotation)
   */
  public void setSharedKey(byte[] key, int keyIndex) {
    options.getKeyProvider().setSharedKey(key, keyIndex);
    notifyKeySet(keyIndex);
  }

  /**
   * Set an encryption key for a specific participant.
   *
   * @param key the raw key bytes
   * @param keyIndex the key index
   * @param participantIdentity the participant's identity
   */
  public void setKey(byte[] key, int keyIndex, String participantIdentity) {
    options.getKeyProvider().setKey(key, keyIndex, participantIdentity);
    notifyKeySet(keyIndex);
  }

  /**
   * Rotate to a new encryption key using key ratcheting.
   *
   * @return the new key info
   */
  public KeyInfo rotateKey() {
    KeyInfo newKey = options.getKeyProvider().rotateKey();
    notifyKeyRotated(newKey);
    return newKey;
  }

  /**
   * Ratchet the shared key to derive a new key.
   *
   * @return the new derived key bytes
   */
  public byte[] ratchetSharedKey() {
    KeyProvider keyProvider = options.getKeyProvider();
    int currentIndex = keyProvider.getLatestKeyIndex("shared");
    byte[] newKey = keyProvider.ratchetSharedKey(currentIndex);
    if (newKey != null) {
      notifyKeyRatcheted("shared", currentIndex + 1);
    }
    return newKey;
  }

  /**
   * Encrypt a data packet for transmission.
   *
   * @param payload the plaintext payload
   * @return encrypted packet, or null if encryption is disabled or fails
   */
  public EncryptedPacket encrypt(byte[] payload) {
    if (!isDataChannelEncryptionEnabled()) {
      return null;
    }
    String identity = localIdentity != null ? localIdentity : "local";
    int keyIndex = options.getKeyProvider().getLatestKeyIndex(identity);
    return dataPacketCryptor.encrypt(identity, keyIndex, payload);
  }

  /**
   * Decrypt a received data packet.
   *
   * @param participantIdentity the sender's identity
   * @param packet the encrypted packet
   * @return decrypted payload, or null if decryption fails
   */
  public byte[] decrypt(String participantIdentity, EncryptedPacket packet) {
    byte[] result = dataPacketCryptor.decrypt(participantIdentity, packet);
    if (result == null) {
      notifyEncryptionError(
          participantIdentity, new E2EEException("Decryption failed for " + participantIdentity));
    }
    return result;
  }

  public void addListener(E2EEListener listener) {
    listeners.add(listener);
  }

  public void removeListener(E2EEListener listener) {
    listeners.remove(listener);
  }

  /** Clean up resources when leaving a room. */
  public void cleanup() {
    // No native resources to clean up in pure Java implementation
  }

  private void notifyStateChanged(boolean enabled) {
    for (E2EEListener listener : listeners) {
      listener.onE2EEStateChanged(enabled);
    }
  }

  private void notifyKeySet(int keyIndex) {
    for (E2EEListener listener : listeners) {
      listener.onKeySet(keyIndex);
    }
  }

  private void notifyKeyRotated(KeyInfo newKey) {
    for (E2EEListener listener : listeners) {
      listener.onKeyRotated(newKey);
    }
  }

  private void notifyKeyRatcheted(String participantIdentity, int keyIndex) {
    for (E2EEListener listener : listeners) {
      listener.onKeyRatcheted(participantIdentity, keyIndex);
    }
  }

  public void notifyEncryptionError(String participantIdentity, E2EEException error) {
    for (E2EEListener listener : listeners) {
      listener.onEncryptionError(participantIdentity, error);
    }
  }
}
