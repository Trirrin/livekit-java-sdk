package io.livekit.sdk;

import livekit.LivekitModels;
import livekit.LivekitRtc;

/**
 * Outbound transport used by Room and participants to send requests to the server. Implemented by
 * the RTC layer, which owns the signaling connection and data channels.
 */
public interface RoomTransport {

  /** Update the local participant's metadata, name, or attributes. */
  void sendUpdateMetadata(LivekitRtc.UpdateParticipantMetadata metadata);

  /** Subscribe to or unsubscribe from remote tracks. */
  void sendUpdateSubscription(LivekitRtc.UpdateSubscription subscription);

  /** Update settings of subscribed tracks (dimensions, fps, paused state). */
  void sendUpdateTrackSettings(LivekitRtc.UpdateTrackSettings settings);

  /** Update which participants are allowed to subscribe to local tracks. */
  void sendSubscriptionPermission(LivekitRtc.SubscriptionPermission permission);

  /** Mute or unmute a published local track. */
  void sendMuteTrack(String trackSid, boolean muted);

  /**
   * Send a data packet over the appropriate data channel.
   *
   * @return true if the packet was handed to an open data channel
   */
  boolean sendDataPacket(LivekitModels.DataPacket packet, boolean reliable);
}
