package io.livekit.sdk;

/**
 * Handler for track subscription events from the RTC layer. This interface bridges the RTC module
 * with the Room model.
 */
public interface TrackSubscriptionHandler {

  /**
   * Called when a remote track is subscribed (media data flowing).
   *
   * @param trackSid The track's SID from the signaling layer
   * @param track The subscribed track instance
   */
  void onTrackSubscribed(String trackSid, Track track);

  /**
   * Called when a remote track is unsubscribed.
   *
   * @param trackSid The track's SID
   * @param track The unsubscribed track instance
   */
  void onTrackUnsubscribed(String trackSid, Track track);

  /**
   * Called when data is received via data channel.
   *
   * @param data The received data
   * @param kind RELIABLE or LOSSY
   * @param participantSid The sender's participant SID (may be null)
   * @param topic The topic (may be null)
   */
  void onDataReceived(byte[] data, DataPacket.Kind kind, String participantSid, String topic);
}
