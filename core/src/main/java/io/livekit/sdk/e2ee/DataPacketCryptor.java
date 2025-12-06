package io.livekit.sdk.e2ee;

import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * AES-GCM cryptor for data channel packets. Provides end-to-end encryption for DataChannel messages
 * independent of WebRTC's native encryption.
 */
public class DataPacketCryptor {
  private static final String ALGORITHM = "AES/GCM/NoPadding";
  private static final int GCM_TAG_LENGTH = 128;
  private static final int IV_LENGTH = 12;

  private final KeyProvider keyProvider;
  private final SecureRandom random = new SecureRandom();
  private volatile boolean enabled = true;

  public DataPacketCryptor(KeyProvider keyProvider) {
    this.keyProvider = keyProvider;
  }

  /**
   * Encrypt a payload using AES-GCM.
   *
   * @param participantIdentity the sender's identity
   * @param keyIndex the key index to use
   * @param payload the plaintext payload
   * @return encrypted packet with IV and key index, or null on failure
   */
  public EncryptedPacket encrypt(String participantIdentity, int keyIndex, byte[] payload) {
    if (!enabled) {
      return null;
    }

    try {
      KeyInfo keyInfo = keyProvider.getKey(participantIdentity, keyIndex);
      if (keyInfo == null) {
        keyInfo = keyProvider.getSharedKey();
      }
      if (keyInfo == null) {
        return null;
      }

      byte[] iv = new byte[IV_LENGTH];
      random.nextBytes(iv);

      Cipher cipher = Cipher.getInstance(ALGORITHM);
      SecretKeySpec keySpec = new SecretKeySpec(keyInfo.getKey(), "AES");
      GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
      cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);

      byte[] encrypted = cipher.doFinal(payload);
      return new EncryptedPacket(encrypted, iv, keyIndex);
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Decrypt a payload using AES-GCM.
   *
   * @param participantIdentity the sender's identity
   * @param packet the encrypted packet
   * @return decrypted payload, or null on failure
   */
  public byte[] decrypt(String participantIdentity, EncryptedPacket packet) {
    try {
      KeyInfo keyInfo = keyProvider.getKey(participantIdentity, packet.getKeyIndex());
      if (keyInfo == null) {
        keyInfo = keyProvider.getSharedKey();
      }
      if (keyInfo == null) {
        return null;
      }

      Cipher cipher = Cipher.getInstance(ALGORITHM);
      SecretKeySpec keySpec = new SecretKeySpec(keyInfo.getKey(), "AES");
      GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, packet.getIv());
      cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);

      return cipher.doFinal(packet.getPayload());
    } catch (Exception e) {
      return null;
    }
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }
}
