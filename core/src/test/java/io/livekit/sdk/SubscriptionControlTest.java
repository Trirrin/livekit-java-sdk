package io.livekit.sdk;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import livekit.LivekitModels;
import livekit.LivekitRtc;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Tests remote track subscription controls and related room events. */
class SubscriptionControlTest {

  private Room room;
  private RecordingTransport transport;

  @BeforeEach
  void setUp() {
    room = new Room();
    transport = new RecordingTransport();
    room.setTransport(transport);
    room.handleJoinResponse(
        LivekitRtc.JoinResponse.newBuilder()
            .setRoom(LivekitModels.Room.newBuilder().setSid("RM_1").build())
            .setParticipant(
                LivekitModels.ParticipantInfo.newBuilder().setSid("PA_LOCAL").setIdentity("local"))
            .build());
    room.handleParticipantUpdate(
        LivekitRtc.ParticipantUpdate.newBuilder()
            .addParticipants(
                LivekitModels.ParticipantInfo.newBuilder()
                    .setSid("PA_R1")
                    .setIdentity("remote1")
                    .addTracks(
                        LivekitModels.TrackInfo.newBuilder()
                            .setSid("TR_V1")
                            .setName("camera")
                            .setType(LivekitModels.TrackType.VIDEO)
                            .setSource(LivekitModels.TrackSource.CAMERA)))
            .build());
  }

  private RemoteTrackPublication videoPub() {
    RemoteParticipant p = room.getRemoteParticipant("remote1");
    return (RemoteTrackPublication) p.getTrackPublication("TR_V1");
  }

  @Test
  void remoteTracksAreRemotePublications() {
    assertNotNull(videoPub());
    assertEquals("PA_R1", videoPub().getParticipantSid());
  }

  @Test
  void setSubscribedSendsUpdateSubscription() {
    videoPub().setSubscribed(true);

    assertNotNull(transport.lastSubscription);
    assertTrue(transport.lastSubscription.getSubscribe());
    assertEquals(List.of("TR_V1"), transport.lastSubscription.getTrackSidsList());
    assertEquals("PA_R1", transport.lastSubscription.getParticipantTracks(0).getParticipantSid());
    assertTrue(videoPub().isSubscribed());
  }

  @Test
  void setVideoQualitySendsTrackSettings() {
    videoPub().setVideoQuality(VideoQuality.LOW);

    assertNotNull(transport.lastSettings);
    assertEquals(LivekitModels.VideoQuality.LOW, transport.lastSettings.getQuality());
    assertEquals(List.of("TR_V1"), transport.lastSettings.getTrackSidsList());
  }

  @Test
  void setVideoDimensionsSendsTrackSettings() {
    videoPub().setVideoDimensions(640, 360);

    assertNotNull(transport.lastSettings);
    assertEquals(640, transport.lastSettings.getWidth());
    assertEquals(360, transport.lastSettings.getHeight());
  }

  @Test
  void setEnabledFalseSendsDisabled() {
    videoPub().setEnabled(false);

    assertNotNull(transport.lastSettings);
    assertTrue(transport.lastSettings.getDisabled());

    transport.lastSettings = null;
    videoPub().setEnabled(false);
    assertNull(transport.lastSettings, "no-op change should not re-send settings");
  }

  @Test
  void streamStateUpdateFiresEvent() {
    AtomicReference<TrackStreamState> state = new AtomicReference<>();
    room.addListener(
        new RoomListener() {
          @Override
          public void onTrackStreamStateChanged(
              Room r, RemoteTrackPublication pub, TrackStreamState s, Participant p) {
            state.set(s);
          }
        });

    room.handleStreamStateUpdate(
        LivekitRtc.StreamStateUpdate.newBuilder()
            .addStreamStates(
                LivekitRtc.StreamStateInfo.newBuilder()
                    .setParticipantSid("PA_R1")
                    .setTrackSid("TR_V1")
                    .setState(LivekitRtc.StreamState.PAUSED))
            .build());

    assertEquals(TrackStreamState.PAUSED, state.get());
    assertEquals(TrackStreamState.PAUSED, videoPub().getStreamState());
  }

  @Test
  void subscriptionPermissionUpdateFiresEvent() {
    AtomicReference<Boolean> allowed = new AtomicReference<>();
    room.addListener(
        new RoomListener() {
          @Override
          public void onTrackSubscriptionPermissionChanged(
              Room r, RemoteTrackPublication pub, Participant p, boolean isAllowed) {
            allowed.set(isAllowed);
          }
        });

    room.handleSubscriptionPermissionUpdate(
        LivekitRtc.SubscriptionPermissionUpdate.newBuilder()
            .setParticipantSid("PA_R1")
            .setTrackSid("TR_V1")
            .setAllowed(false)
            .build());

    assertEquals(Boolean.FALSE, allowed.get());
    assertFalse(videoPub().isSubscriptionAllowed());
  }

  @Test
  void setTrackSubscriptionPermissionsSendsRequest() {
    room.getLocalParticipant()
        .setTrackSubscriptionPermissions(
            false, List.of(new ParticipantTrackPermission("remote1", true, null)));

    assertNotNull(transport.lastPermission);
    assertFalse(transport.lastPermission.getAllParticipants());
    assertEquals(
        "remote1", transport.lastPermission.getTrackPermissions(0).getParticipantIdentity());
    assertTrue(transport.lastPermission.getTrackPermissions(0).getAllTracks());
  }

  @Test
  void requestResponseErrorFiresEvent() {
    AtomicReference<String> reason = new AtomicReference<>();
    room.addListener(
        new RoomListener() {
          @Override
          public void onSignalRequestError(Room r, long requestId, String rsn, String message) {
            reason.set(rsn);
          }
        });

    room.handleRequestResponse(
        LivekitRtc.RequestResponse.newBuilder()
            .setRequestId(7)
            .setReason(LivekitRtc.RequestResponse.Reason.NOT_ALLOWED)
            .build());
    assertEquals("NOT_ALLOWED", reason.get());

    reason.set(null);
    room.handleRequestResponse(
        LivekitRtc.RequestResponse.newBuilder()
            .setRequestId(8)
            .setReason(LivekitRtc.RequestResponse.Reason.OK)
            .build());
    assertNull(reason.get(), "OK responses should not fire the error event");
  }

  static class RecordingTransport implements RoomTransport {
    LivekitRtc.UpdateSubscription lastSubscription;
    LivekitRtc.UpdateTrackSettings lastSettings;
    LivekitRtc.SubscriptionPermission lastPermission;

    @Override
    public void sendUpdateMetadata(LivekitRtc.UpdateParticipantMetadata metadata) {}

    @Override
    public void sendUpdateSubscription(LivekitRtc.UpdateSubscription subscription) {
      lastSubscription = subscription;
    }

    @Override
    public void sendUpdateTrackSettings(LivekitRtc.UpdateTrackSettings settings) {
      lastSettings = settings;
    }

    @Override
    public void sendSubscriptionPermission(LivekitRtc.SubscriptionPermission permission) {
      lastPermission = permission;
    }

    @Override
    public void sendMuteTrack(String trackSid, boolean muted) {}

    @Override
    public boolean sendDataPacket(LivekitModels.DataPacket packet, boolean reliable) {
      return true;
    }
  }
}
