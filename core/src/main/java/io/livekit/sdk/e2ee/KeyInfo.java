package io.livekit.sdk.e2ee;

/** Represents an encryption key with its metadata. */
public class KeyInfo {
  private final byte[] key;
  private final int keyIndex;
  private final String participantIdentity;

  public KeyInfo(byte[] key, int keyIndex, String participantIdentity) {
    this.key = key.clone();
    this.keyIndex = keyIndex;
    this.participantIdentity = participantIdentity;
  }

  public byte[] getKey() {
    return key.clone();
  }

  public int getKeyIndex() {
    return keyIndex;
  }

  public String getParticipantIdentity() {
    return participantIdentity;
  }
}
