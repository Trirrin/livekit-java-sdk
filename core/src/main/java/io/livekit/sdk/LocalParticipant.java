package io.livekit.sdk;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Represents the local participant (current user) in the room. */
public class LocalParticipant extends Participant {
  private final Map<String, TrackPublication> localTrackPublications;
  private LocalTrackManager trackManager;
  private RoomTransport transport;

  public LocalParticipant(String sid, String identity) {
    super(sid, identity);
    this.localTrackPublications = new ConcurrentHashMap<>();
  }

  /** Set the track manager for controlling local media tracks. */
  public void setTrackManager(LocalTrackManager trackManager) {
    this.trackManager = trackManager;
  }

  void setTransport(RoomTransport transport) {
    this.transport = transport;
  }

  RoomTransport getTransport() {
    return transport;
  }

  /** Get the track manager. */
  public LocalTrackManager getTrackManager() {
    return trackManager;
  }

  /**
   * Publish a local track to the room. This is a placeholder - actual implementation requires RTC
   * transport.
   */
  public void publishTrack(Track track) {
    // Will be implemented with RTC transport
  }

  /** Unpublish a local track. */
  public void unpublishTrack(Track track) {
    // Will be implemented with RTC transport
  }

  /** Set microphone enabled state. */
  public void setMicrophoneEnabled(boolean enabled) {
    if (trackManager != null) {
      trackManager.setMicrophoneEnabled(enabled);
    }
  }

  /** Set camera enabled state. */
  public void setCameraEnabled(boolean enabled) {
    if (trackManager != null) {
      trackManager.setCameraEnabled(enabled);
    }
  }

  /** Set screen share enabled state. */
  public void setScreenShareEnabled(boolean enabled) {
    if (trackManager != null) {
      trackManager.setScreenShareEnabled(enabled);
    }
  }

  /** Check if microphone is enabled. */
  public boolean isMicrophoneEnabled() {
    return trackManager != null && trackManager.isMicrophoneEnabled();
  }

  /** Check if camera is enabled. */
  public boolean isCameraEnabled() {
    return trackManager != null && trackManager.isCameraEnabled();
  }

  /** Check if screen share is enabled. */
  public boolean isScreenShareEnabled() {
    return trackManager != null && trackManager.isScreenShareEnabled();
  }

  /**
   * Update the local participant's metadata on the server. Requires the
   * canUpdateOwnParticipantMetadata permission. Local state is updated when the server confirms via
   * a participant update, which also fires {@code onParticipantMetadataChanged}.
   */
  public void updateMetadata(String metadata) {
    sendMetadataUpdate(metadata, null, null);
  }

  /** Update the local participant's display name on the server. */
  public void updateName(String name) {
    sendMetadataUpdate(null, name, null);
  }

  /**
   * Update the local participant's attributes on the server. Only the provided keys are updated; to
   * delete an attribute, set its value to an empty string.
   */
  public void updateAttributes(Map<String, String> attributes) {
    sendMetadataUpdate(null, null, attributes);
  }

  private void sendMetadataUpdate(String metadata, String name, Map<String, String> attributes) {
    if (transport == null) {
      throw new IllegalStateException("Not connected: no transport available");
    }
    transport.sendUpdateMetadata(ProtoConverter.buildMetadataUpdate(metadata, name, attributes));
  }

  /**
   * @deprecated Use {@link #updateMetadata(String)}.
   */
  @Deprecated
  public void setParticipantMetadata(String metadata) {
    updateMetadata(metadata);
  }

  /**
   * @deprecated Use {@link #updateName(String)}.
   */
  @Deprecated
  public void setParticipantName(String name) {
    updateName(name);
  }

  /**
   * @deprecated Use {@link #updateAttributes(Map)}.
   */
  @Deprecated
  public void setParticipantAttributes(Map<String, String> attributes) {
    updateAttributes(attributes);
  }
}
