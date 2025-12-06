package io.livekit.sdk.e2ee;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages E2EE state and coordinates encryption/decryption for a room. This is the main entry point
 * for E2EE functionality.
 */
public class E2EEManager {
  private final E2EEOptions options;
  private final List<E2EEListener> listeners = new CopyOnWriteArrayList<>();
  private boolean enabled;

  public E2EEManager(E2EEOptions options) {
    this.options = options;
    this.enabled = true;
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
    notifyStateChanged(enabled);
  }

  /**
   * Set the encryption key for the local participant.
   *
   * @param key the raw key bytes
   * @param keyIndex the key index (for key rotation)
   */
  public void setKey(byte[] key, int keyIndex) {
    options.getKeyProvider().setSharedKey(key, keyIndex);
    notifyKeySet(keyIndex);
  }

  /**
   * Rotate to a new encryption key. The new key will be used for encrypting outgoing frames.
   *
   * @return the new key info
   */
  public KeyInfo rotateKey() {
    KeyInfo newKey = options.getKeyProvider().rotateKey();
    notifyKeyRotated(newKey);
    return newKey;
  }

  /**
   * Ratchet the key for a participant. Used when receiving frames encrypted with a newer key index.
   *
   * @param participantIdentity the participant's identity
   * @param keyIndex the new key index
   */
  public void ratchetKey(String participantIdentity, int keyIndex) {
    notifyKeyRatcheted(participantIdentity, keyIndex);
  }

  public void addListener(E2EEListener listener) {
    listeners.add(listener);
  }

  public void removeListener(E2EEListener listener) {
    listeners.remove(listener);
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
