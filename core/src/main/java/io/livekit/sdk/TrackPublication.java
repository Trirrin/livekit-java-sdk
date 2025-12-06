package io.livekit.sdk;

/**
 * Represents a track publication within a room. A publication is a track that is published to the
 * room.
 */
public class TrackPublication {
  protected final String sid;
  protected final String name;
  protected final TrackType type;
  protected TrackSource source;
  protected Track track;
  protected boolean muted;
  protected boolean subscribed;
  protected String mimeType;

  public TrackPublication(String sid, String name, TrackType type) {
    this.sid = sid;
    this.name = name;
    this.type = type;
    this.source = TrackSource.UNKNOWN;
  }

  public String getSid() {
    return sid;
  }

  public String getName() {
    return name;
  }

  public TrackType getType() {
    return type;
  }

  public TrackSource getSource() {
    return source;
  }

  public void setSource(TrackSource source) {
    this.source = source;
  }

  public Track getTrack() {
    return track;
  }

  public void setTrack(Track track) {
    this.track = track;
  }

  public boolean isMuted() {
    return muted;
  }

  public void setMuted(boolean muted) {
    this.muted = muted;
  }

  public boolean isSubscribed() {
    return subscribed;
  }

  public void setSubscribed(boolean subscribed) {
    this.subscribed = subscribed;
  }

  public String getMimeType() {
    return mimeType;
  }

  public void setMimeType(String mimeType) {
    this.mimeType = mimeType;
  }
}
