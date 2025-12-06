package io.livekit.sdk.rtc;

import dev.onvoid.webrtc.media.audio.AudioTrack;

/** Wrapper for a remote audio track received via WebRTC. */
public class RemoteAudioTrack extends io.livekit.sdk.AudioTrack {
  private final AudioTrack nativeTrack;

  public RemoteAudioTrack(String sid, String name, AudioTrack nativeTrack) {
    super(sid, name);
    this.nativeTrack = nativeTrack;
  }

  /** Get the native WebRTC audio track. */
  public AudioTrack getNativeTrack() {
    return nativeTrack;
  }

  /** Enable or disable audio playback. */
  public void setEnabled(boolean enabled) {
    if (nativeTrack != null) {
      nativeTrack.setEnabled(enabled);
    }
  }

  /** Check if audio playback is enabled. */
  public boolean isEnabled() {
    return nativeTrack != null && nativeTrack.isEnabled();
  }

  /** Dispose of native resources. */
  public void dispose() {
    if (nativeTrack != null) {
      nativeTrack.dispose();
    }
  }
}
