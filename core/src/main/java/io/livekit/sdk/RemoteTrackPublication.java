package io.livekit.sdk;

import livekit.LivekitRtc;

/**
 * A track published by a remote participant. Exposes subscription controls: subscribe/unsubscribe,
 * pausing, and requesting specific video quality, dimensions, or frame rate from the SFU.
 */
public class RemoteTrackPublication extends TrackPublication {

  private RoomTransport transport;
  private String participantSid;

  private TrackStreamState streamState = TrackStreamState.UNKNOWN;
  private boolean subscriptionAllowed = true;

  // Requested track settings, re-sent as a whole on every change
  private boolean disabled;
  private VideoQuality requestedQuality;
  private int requestedWidth;
  private int requestedHeight;
  private int requestedFps;

  public RemoteTrackPublication(String sid, String name, TrackType type) {
    super(sid, name, type);
  }

  void attach(RoomTransport transport, String participantSid) {
    this.transport = transport;
    this.participantSid = participantSid;
  }

  public String getParticipantSid() {
    return participantSid;
  }

  /** Stream state reported by the server; PAUSED when the server pauses under congestion. */
  public TrackStreamState getStreamState() {
    return streamState;
  }

  void setStreamState(TrackStreamState streamState) {
    this.streamState = streamState;
  }

  /** Whether the local participant is allowed to subscribe to this track. */
  public boolean isSubscriptionAllowed() {
    return subscriptionAllowed;
  }

  void setSubscriptionAllowed(boolean subscriptionAllowed) {
    this.subscriptionAllowed = subscriptionAllowed;
  }

  /** Subscribe to or unsubscribe from this track. */
  public void setSubscribed(boolean subscribe) {
    requireTransport();
    LivekitRtc.UpdateSubscription.Builder builder =
        LivekitRtc.UpdateSubscription.newBuilder().addTrackSids(sid).setSubscribe(subscribe);
    if (participantSid != null) {
      builder.addParticipantTracks(
          livekit.LivekitModels.ParticipantTracks.newBuilder()
              .setParticipantSid(participantSid)
              .addTrackSids(sid)
              .build());
    }
    transport.sendUpdateSubscription(builder.build());
    this.subscribed = subscribe;
  }

  /**
   * Enable or disable delivery of this track without changing the subscription. Disabling pauses
   * data from the server while keeping the subscription active.
   */
  public void setEnabled(boolean enabled) {
    if (this.disabled == !enabled) {
      return;
    }
    this.disabled = !enabled;
    sendTrackSettings();
  }

  /** Request a specific video quality (simulcast layer) from the server. */
  public void setVideoQuality(VideoQuality quality) {
    if (quality == this.requestedQuality) {
      return;
    }
    this.requestedQuality = quality;
    this.requestedWidth = 0;
    this.requestedHeight = 0;
    sendTrackSettings();
  }

  /** Request video dimensions from the server; it picks the closest available layer. */
  public void setVideoDimensions(int width, int height) {
    if (width == this.requestedWidth && height == this.requestedHeight) {
      return;
    }
    this.requestedWidth = width;
    this.requestedHeight = height;
    this.requestedQuality = null;
    sendTrackSettings();
  }

  /** Request a maximum frame rate from the server. */
  public void setVideoFps(int fps) {
    if (fps == this.requestedFps) {
      return;
    }
    this.requestedFps = fps;
    sendTrackSettings();
  }

  private void sendTrackSettings() {
    requireTransport();
    LivekitRtc.UpdateTrackSettings.Builder builder =
        LivekitRtc.UpdateTrackSettings.newBuilder().addTrackSids(sid).setDisabled(disabled);
    if (requestedQuality != null) {
      builder.setQuality(requestedQuality.toProto());
    }
    if (requestedWidth > 0 && requestedHeight > 0) {
      builder.setWidth(requestedWidth).setHeight(requestedHeight);
    }
    if (requestedFps > 0) {
      builder.setFps(requestedFps);
    }
    transport.sendUpdateTrackSettings(builder.build());
  }

  private void requireTransport() {
    if (transport == null) {
      throw new IllegalStateException("Publication is not attached to a connected room");
    }
  }
}
