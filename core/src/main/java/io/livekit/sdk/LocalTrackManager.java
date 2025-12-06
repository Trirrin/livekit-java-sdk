package io.livekit.sdk;

/**
 * Interface for managing local track state. Used by LocalParticipant to delegate media control
 * operations to the RTC layer.
 */
public interface LocalTrackManager {

  /** Enable or disable the microphone track. */
  void setMicrophoneEnabled(boolean enabled);

  /** Enable or disable the camera track. */
  void setCameraEnabled(boolean enabled);

  /** Enable or disable screen sharing. */
  void setScreenShareEnabled(boolean enabled);

  /** Check if the microphone track is currently enabled. */
  boolean isMicrophoneEnabled();

  /** Check if the camera track is currently enabled. */
  boolean isCameraEnabled();

  /** Check if screen sharing is currently enabled. */
  boolean isScreenShareEnabled();
}
