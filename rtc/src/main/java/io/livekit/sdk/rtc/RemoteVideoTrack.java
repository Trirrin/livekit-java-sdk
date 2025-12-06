package io.livekit.sdk.rtc;

import dev.onvoid.webrtc.media.video.VideoTrack;
import dev.onvoid.webrtc.media.video.VideoTrackSink;

/** Wrapper for a remote video track received via WebRTC. */
public class RemoteVideoTrack extends io.livekit.sdk.VideoTrack {
  private final VideoTrack nativeTrack;

  public RemoteVideoTrack(String sid, String name, VideoTrack nativeTrack) {
    super(sid, name);
    this.nativeTrack = nativeTrack;
  }

  /** Get the native WebRTC video track. */
  public VideoTrack getNativeTrack() {
    return nativeTrack;
  }

  /** Enable or disable video rendering. */
  public void setEnabled(boolean enabled) {
    if (nativeTrack != null) {
      nativeTrack.setEnabled(enabled);
    }
  }

  /** Check if video rendering is enabled. */
  public boolean isEnabled() {
    return nativeTrack != null && nativeTrack.isEnabled();
  }

  /** Add a video sink to receive video frames. */
  public void addSink(VideoTrackSink sink) {
    if (nativeTrack != null) {
      nativeTrack.addSink(sink);
    }
  }

  /** Remove a video sink. */
  public void removeSink(VideoTrackSink sink) {
    if (nativeTrack != null) {
      nativeTrack.removeSink(sink);
    }
  }

  /** Dispose of native resources. */
  public void dispose() {
    if (nativeTrack != null) {
      nativeTrack.dispose();
    }
  }
}
