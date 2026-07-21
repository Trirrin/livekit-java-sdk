package io.livekit.sdk;

/** Listener interface for room events. */
public interface RoomListener {

  default void onConnected(Room room) {}

  default void onDisconnected(Room room, DisconnectReason reason) {}

  default void onReconnecting(Room room) {}

  default void onReconnected(Room room) {}

  default void onParticipantConnected(Room room, RemoteParticipant participant) {}

  default void onParticipantDisconnected(Room room, RemoteParticipant participant) {}

  default void onParticipantMetadataChanged(
      Room room, Participant participant, String prevMetadata) {}

  default void onParticipantNameChanged(Room room, Participant participant, String prevName) {}

  default void onParticipantAttributesChanged(
      Room room, Participant participant, java.util.Map<String, String> prevAttributes) {}

  default void onTrackPublished(Room room, TrackPublication publication, Participant participant) {}

  default void onTrackUnpublished(
      Room room, TrackPublication publication, Participant participant) {}

  default void onTrackSubscribed(
      Room room, Track track, TrackPublication publication, RemoteParticipant participant) {}

  default void onTrackUnsubscribed(
      Room room, Track track, TrackPublication publication, RemoteParticipant participant) {}

  default void onTrackMuted(Room room, TrackPublication publication, Participant participant) {}

  default void onTrackUnmuted(Room room, TrackPublication publication, Participant participant) {}

  default void onDataReceived(
      Room room, byte[] data, RemoteParticipant participant, DataPacket.Kind kind, String topic) {}

  default void onActiveSpeakersChanged(Room room, java.util.List<Participant> speakers) {}

  default void onConnectionQualityChanged(
      Room room, Participant participant, ConnectionQuality quality) {}

  default void onRoomMetadataChanged(Room room, String metadata) {}

  /** Called when the server pauses or resumes a subscribed track (e.g. due to congestion). */
  default void onTrackStreamStateChanged(
      Room room,
      RemoteTrackPublication publication,
      TrackStreamState state,
      Participant participant) {}

  /** Called when permission to subscribe to a remote track was granted or revoked. */
  default void onTrackSubscriptionPermissionChanged(
      Room room, RemoteTrackPublication publication, Participant participant, boolean allowed) {}

  /** Called when a track subscription failed on the server. */
  default void onTrackSubscriptionFailed(Room room, String trackSid, String error) {}

  /** Called when a signal request was rejected by the server. */
  default void onSignalRequestError(Room room, long requestId, String reason, String message) {}

  /** Called when one of the local participant's tracks was subscribed for the first time. */
  default void onLocalTrackSubscribed(Room room, TrackPublication publication) {}

  /** Called after the server moved this participant to a different room. */
  default void onRoomMoved(Room room) {}
}
