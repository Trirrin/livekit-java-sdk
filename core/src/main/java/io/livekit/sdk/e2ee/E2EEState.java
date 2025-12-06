package io.livekit.sdk.e2ee;

/** Encryption state for E2EE tracks and data channels. */
public enum E2EEState {
  NEW,
  OK,
  KEY_RATCHETED,
  MISSING_KEY,
  ENCRYPTION_FAILED,
  DECRYPTION_FAILED,
  INTERNAL_ERROR
}
