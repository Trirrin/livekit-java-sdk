package io.livekit.sdk.rtc;

/** Common interface for publishable local media tracks. */
public interface LocalMediaTrack {

  /** Client-generated track id (cid), also used as the native track id. */
  String getId();

  /** Track name sent to the server. */
  String getName();

  /** Server-assigned track sid, available after the track has been published. */
  String getSid();

  void setSid(String sid);

  boolean isMuted();

  void setMuted(boolean muted);

  void dispose();
}
