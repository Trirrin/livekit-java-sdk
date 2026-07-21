package io.livekit.sdk.signaling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicReference;
import livekit.LivekitRtc;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Tests signal response dispatching to listeners. */
class SignalClientResponseTest {

  private SignalClient client;

  @BeforeEach
  void setUp() {
    client = new SignalClient();
  }

  @Test
  void dispatchesRequestResponse() {
    AtomicReference<LivekitRtc.RequestResponse> received = new AtomicReference<>();
    client.addListener(
        new SignalListenerAdapter() {
          @Override
          public void onRequestResponse(LivekitRtc.RequestResponse response) {
            received.set(response);
          }
        });

    LivekitRtc.RequestResponse response =
        LivekitRtc.RequestResponse.newBuilder()
            .setRequestId(42)
            .setReason(LivekitRtc.RequestResponse.Reason.NOT_ALLOWED)
            .setMessage("not allowed")
            .build();
    client.processSignalResponse(
        LivekitRtc.SignalResponse.newBuilder().setRequestResponse(response).build());

    assertNotNull(received.get());
    assertEquals(42, received.get().getRequestId());
    assertEquals(LivekitRtc.RequestResponse.Reason.NOT_ALLOWED, received.get().getReason());
  }

  @Test
  void dispatchesSubscribedQualityUpdate() {
    AtomicReference<LivekitRtc.SubscribedQualityUpdate> received = new AtomicReference<>();
    client.addListener(
        new SignalListenerAdapter() {
          @Override
          public void onSubscribedQualityUpdate(LivekitRtc.SubscribedQualityUpdate update) {
            received.set(update);
          }
        });

    LivekitRtc.SubscribedQualityUpdate update =
        LivekitRtc.SubscribedQualityUpdate.newBuilder().setTrackSid("TR_123").build();
    client.processSignalResponse(
        LivekitRtc.SignalResponse.newBuilder().setSubscribedQualityUpdate(update).build());

    assertNotNull(received.get());
    assertEquals("TR_123", received.get().getTrackSid());
  }

  @Test
  void dispatchesSubscriptionPermissionUpdate() {
    AtomicReference<LivekitRtc.SubscriptionPermissionUpdate> received = new AtomicReference<>();
    client.addListener(
        new SignalListenerAdapter() {
          @Override
          public void onSubscriptionPermissionUpdate(
              LivekitRtc.SubscriptionPermissionUpdate update) {
            received.set(update);
          }
        });

    LivekitRtc.SubscriptionPermissionUpdate update =
        LivekitRtc.SubscriptionPermissionUpdate.newBuilder()
            .setParticipantSid("PA_1")
            .setTrackSid("TR_1")
            .setAllowed(false)
            .build();
    client.processSignalResponse(
        LivekitRtc.SignalResponse.newBuilder().setSubscriptionPermissionUpdate(update).build());

    assertNotNull(received.get());
    assertEquals("PA_1", received.get().getParticipantSid());
    assertEquals(false, received.get().getAllowed());
  }

  @Test
  void dispatchesSubscriptionResponse() {
    AtomicReference<LivekitRtc.SubscriptionResponse> received = new AtomicReference<>();
    client.addListener(
        new SignalListenerAdapter() {
          @Override
          public void onSubscriptionResponse(LivekitRtc.SubscriptionResponse response) {
            received.set(response);
          }
        });

    LivekitRtc.SubscriptionResponse response =
        LivekitRtc.SubscriptionResponse.newBuilder()
            .setTrackSid("TR_9")
            .setErr(livekit.LivekitModels.SubscriptionError.SE_TRACK_NOTFOUND)
            .build();
    client.processSignalResponse(
        LivekitRtc.SignalResponse.newBuilder().setSubscriptionResponse(response).build());

    assertNotNull(received.get());
    assertEquals("TR_9", received.get().getTrackSid());
  }

  @Test
  void dispatchesLocalTrackSubscribed() {
    AtomicReference<LivekitRtc.TrackSubscribed> received = new AtomicReference<>();
    client.addListener(
        new SignalListenerAdapter() {
          @Override
          public void onLocalTrackSubscribed(LivekitRtc.TrackSubscribed trackSubscribed) {
            received.set(trackSubscribed);
          }
        });

    client.processSignalResponse(
        LivekitRtc.SignalResponse.newBuilder()
            .setTrackSubscribed(
                LivekitRtc.TrackSubscribed.newBuilder().setTrackSid("TR_LOCAL").build())
            .build());

    assertNotNull(received.get());
    assertEquals("TR_LOCAL", received.get().getTrackSid());
  }

  @Test
  void dispatchesRoomMoved() {
    AtomicReference<LivekitRtc.RoomMovedResponse> received = new AtomicReference<>();
    client.addListener(
        new SignalListenerAdapter() {
          @Override
          public void onRoomMoved(LivekitRtc.RoomMovedResponse moved) {
            received.set(moved);
          }
        });

    LivekitRtc.RoomMovedResponse moved =
        LivekitRtc.RoomMovedResponse.newBuilder()
            .setRoom(livekit.LivekitModels.Room.newBuilder().setName("new-room").build())
            .setToken("new-token")
            .build();
    client.processSignalResponse(
        LivekitRtc.SignalResponse.newBuilder().setRoomMoved(moved).build());

    assertNotNull(received.get());
    assertEquals("new-room", received.get().getRoom().getName());
  }

  @Test
  void protocolVersionMatchesOfficialClients() {
    assertTrue(SignalClient.PROTOCOL_VERSION >= 15, "protocol must support RPC and request_id");
    assertEquals(17, SignalClient.PROTOCOL_VERSION);
  }

  /** Adapter with empty implementations of the abstract listener methods. */
  abstract static class SignalListenerAdapter implements SignalListener {
    @Override
    public void onStateChanged(SignalState state) {}

    @Override
    public void onJoinResponse(LivekitRtc.JoinResponse response) {}

    @Override
    public void onAnswer(LivekitRtc.SessionDescription answer) {}

    @Override
    public void onOffer(LivekitRtc.SessionDescription offer) {}

    @Override
    public void onTrickle(LivekitRtc.TrickleRequest trickle) {}

    @Override
    public void onParticipantUpdate(LivekitRtc.ParticipantUpdate update) {}

    @Override
    public void onTrackPublished(LivekitRtc.TrackPublishedResponse response) {}

    @Override
    public void onTrackUnpublished(LivekitRtc.TrackUnpublishedResponse response) {}

    @Override
    public void onLeave(LivekitRtc.LeaveRequest leave) {}

    @Override
    public void onMuteTrack(LivekitRtc.MuteTrackRequest mute) {}

    @Override
    public void onSpeakersChanged(LivekitRtc.SpeakersChanged speakersChanged) {}

    @Override
    public void onRoomUpdate(LivekitRtc.RoomUpdate roomUpdate) {}

    @Override
    public void onConnectionQuality(LivekitRtc.ConnectionQualityUpdate quality) {}

    @Override
    public void onStreamStateUpdate(LivekitRtc.StreamStateUpdate streamState) {}

    @Override
    public void onRefreshToken(String token) {}

    @Override
    public void onReconnectResponse(LivekitRtc.ReconnectResponse response) {}

    @Override
    public void onPong(long timestamp) {}

    @Override
    public void onError(Exception e) {}
  }
}
