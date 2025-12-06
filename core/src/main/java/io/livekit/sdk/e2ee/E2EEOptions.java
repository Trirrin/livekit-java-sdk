package io.livekit.sdk.e2ee;

/** Configuration options for E2EE. Based on LiveKit Android SDK's E2EEOptions. */
public class E2EEOptions {
  private final EncryptionType encryptionType;
  private final KeyProvider keyProvider;

  /** Create E2EE options with default BaseKeyProvider in shared key mode. */
  public E2EEOptions() {
    this(EncryptionType.GCM, new BaseKeyProvider(true));
  }

  /** Create E2EE options with specified encryption type and default key provider. */
  public E2EEOptions(EncryptionType encryptionType) {
    this(encryptionType, new BaseKeyProvider(true));
  }

  /** Create E2EE options with specified key provider. */
  public E2EEOptions(KeyProvider keyProvider) {
    this(EncryptionType.GCM, keyProvider);
  }

  /** Create E2EE options with specified encryption type and key provider. */
  public E2EEOptions(EncryptionType encryptionType, KeyProvider keyProvider) {
    this.encryptionType = encryptionType;
    this.keyProvider = keyProvider;
  }

  public EncryptionType getEncryptionType() {
    return encryptionType;
  }

  public KeyProvider getKeyProvider() {
    return keyProvider;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private EncryptionType encryptionType = EncryptionType.GCM;
    private KeyProvider keyProvider;
    private String ratchetSalt = BaseKeyProvider.DEFAULT_RATCHET_SALT;
    private int ratchetWindowSize = BaseKeyProvider.DEFAULT_RATCHET_WINDOW_SIZE;
    private boolean sharedKeyMode = true;

    public Builder encryptionType(EncryptionType type) {
      this.encryptionType = type;
      return this;
    }

    public Builder keyProvider(KeyProvider provider) {
      this.keyProvider = provider;
      return this;
    }

    public Builder ratchetSalt(String salt) {
      this.ratchetSalt = salt;
      return this;
    }

    public Builder ratchetWindowSize(int size) {
      this.ratchetWindowSize = size;
      return this;
    }

    public Builder sharedKeyMode(boolean sharedKey) {
      this.sharedKeyMode = sharedKey;
      return this;
    }

    public E2EEOptions build() {
      if (keyProvider == null) {
        keyProvider = new BaseKeyProvider(sharedKeyMode, ratchetSalt, ratchetWindowSize);
      }
      return new E2EEOptions(encryptionType, keyProvider);
    }
  }
}
