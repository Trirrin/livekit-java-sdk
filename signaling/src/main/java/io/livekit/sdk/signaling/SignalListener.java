package io.livekit.sdk.signaling;

import java.util.List;
import livekit.LivekitRtc;

/** Listener interface for signaling events. */
public interface SignalListener {

  void onStateChanged(SignalState state);

  void onJoinResponse(LivekitRtc.JoinResponse response);

  void onAnswer(LivekitRtc.SessionDescription answer);

  void onOffer(LivekitRtc.SessionDescription offer);

  void onTrickle(LivekitRtc.TrickleRequest trickle);

  void onParticipantUpdate(LivekitRtc.ParticipantUpdate update);

  void onTrackPublished(LivekitRtc.TrackPublishedResponse response);

  void onTrackUnpublished(LivekitRtc.TrackUnpublishedResponse response);

  void onLeave(LivekitRtc.LeaveRequest leave);

  void onMuteTrack(LivekitRtc.MuteTrackRequest mute);

  void onSpeakersChanged(LivekitRtc.SpeakersChanged speakersChanged);

  void onRoomUpdate(LivekitRtc.RoomUpdate roomUpdate);

  void onConnectionQuality(LivekitRtc.ConnectionQualityUpdate quality);

  void onStreamStateUpdate(LivekitRtc.StreamStateUpdate streamState);

  void onRefreshToken(String token);

  void onReconnectResponse(LivekitRtc.ReconnectResponse response);

  void onPong(long timestamp);

  void onError(Exception e);

  /**
   * Called when ICE servers are updated (from JoinResponse or ReconnectResponse). RTC layer should
   * use these servers for ICE restart.
   */
  default void onIceServersUpdated(List<LivekitRtc.ICEServer> iceServers) {}

  /** Called when an ICE restart should be triggered due to connection issues. */
  default void onIceRestartRequired(ReconnectReason reason) {}

  /** Called when the server responds to a request that carried a request_id. */
  default void onRequestResponse(LivekitRtc.RequestResponse response) {}

  /** Called when the max subscribed quality of a published track changed (dynacast). */
  default void onSubscribedQualityUpdate(LivekitRtc.SubscribedQualityUpdate update) {}

  /** Called when a track subscription permission changed. */
  default void onSubscriptionPermissionUpdate(LivekitRtc.SubscriptionPermissionUpdate update) {}

  /** Called when the server responds to a subscription request, including failures. */
  default void onSubscriptionResponse(LivekitRtc.SubscriptionResponse response) {}

  /** Called when one of the local participant's tracks is subscribed for the first time. */
  default void onLocalTrackSubscribed(LivekitRtc.TrackSubscribed trackSubscribed) {}

  /** Called when the participant has been moved to a new room by the server. */
  default void onRoomMoved(LivekitRtc.RoomMovedResponse moved) {}
}
