package io.livekit.sdk.rtc;

import dev.onvoid.webrtc.media.video.VideoTrack;

/** Wrapper for a local video track to be published. */
public class LocalVideoTrack implements LocalMediaTrack {
  private final String id;
  private final String name;
  private final VideoTrack nativeTrack;
  private final int width;
  private final int height;
  private final Runnable sourceDisposer;
  private String sid;
  private boolean muted;

  public LocalVideoTrack(String id, String name, VideoTrack nativeTrack, int width, int height) {
    this(id, name, nativeTrack, width, height, null);
  }

  /**
   * @param sourceDisposer stops and releases the capture source when the track is disposed
   */
  public LocalVideoTrack(
      String id,
      String name,
      VideoTrack nativeTrack,
      int width,
      int height,
      Runnable sourceDisposer) {
    this.id = id;
    this.name = name;
    this.nativeTrack = nativeTrack;
    this.width = width;
    this.height = height;
    this.sourceDisposer = sourceDisposer;
    this.muted = false;
  }

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getSid() {
    return sid;
  }

  public void setSid(String sid) {
    this.sid = sid;
  }

  public VideoTrack getNativeTrack() {
    return nativeTrack;
  }

  public int getWidth() {
    return width;
  }

  public int getHeight() {
    return height;
  }

  public boolean isMuted() {
    return muted;
  }

  public void setMuted(boolean muted) {
    this.muted = muted;
    if (nativeTrack != null) {
      nativeTrack.setEnabled(!muted);
    }
  }

  public void dispose() {
    if (nativeTrack != null) {
      nativeTrack.dispose();
    }
    if (sourceDisposer != null) {
      sourceDisposer.run();
    }
  }
}
