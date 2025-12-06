package io.livekit.sdk.e2ee;

/** Represents an encrypted data packet with IV and key index for decryption. */
public class EncryptedPacket {
  private final byte[] payload;
  private final byte[] iv;
  private final int keyIndex;

  public EncryptedPacket(byte[] payload, byte[] iv, int keyIndex) {
    this.payload = payload.clone();
    this.iv = iv.clone();
    this.keyIndex = keyIndex;
  }

  public byte[] getPayload() {
    return payload.clone();
  }

  public byte[] getIv() {
    return iv.clone();
  }

  public int getKeyIndex() {
    return keyIndex;
  }
}
