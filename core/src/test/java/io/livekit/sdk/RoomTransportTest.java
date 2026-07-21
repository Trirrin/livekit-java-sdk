package io.livekit.sdk;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import livekit.LivekitModels;
import livekit.LivekitRtc;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Tests the outbound transport seam and participant update change events. */
class RoomTransportTest {

  private Room room;
  private FakeTransport transport;

  @BeforeEach
  void setUp() {
    room = new Room();
    transport = new FakeTransport();
    room.setTransport(transport);
    room.handleJoinResponse(joinResponse());
  }

  private LivekitRtc.JoinResponse joinResponse() {
    return LivekitRtc.JoinResponse.newBuilder()
        .setRoom(LivekitModels.Room.newBuilder().setSid("RM_1").setName("test").build())
        .setParticipant(
            LivekitModels.ParticipantInfo.newBuilder()
                .setSid("PA_LOCAL")
                .setIdentity("local")
                .setName("Local")
                .build())
        .build();
  }

  @Test
  void updateMetadataSendsSignalRequest() {
    room.getLocalParticipant().updateMetadata("new-metadata");

    assertNotNull(transport.lastMetadataUpdate);
    assertEquals("new-metadata", transport.lastMetadataUpdate.getMetadata());
    // Local state is not applied until the server confirms
    assertNotEquals("new-metadata", room.getLocalParticipant().getMetadata());
  }

  @Test
  void updateNameAndAttributesSendSignalRequests() {
    room.getLocalParticipant().updateName("NewName");
    assertEquals("NewName", transport.lastMetadataUpdate.getName());

    room.getLocalParticipant().updateAttributes(Map.of("k", "v"));
    assertEquals("v", transport.lastMetadataUpdate.getAttributesMap().get("k"));
  }

  @Test
  void updateMetadataWithoutTransportThrows() {
    LocalParticipant orphan = new LocalParticipant("PA_X", "x");
    assertThrows(IllegalStateException.class, () -> orphan.updateMetadata("m"));
  }

  @Test
  void participantUpdateFiresMetadataNameAndAttributeEvents() {
    AtomicReference<String> prevMetadata = new AtomicReference<>();
    AtomicReference<String> prevName = new AtomicReference<>();
    AtomicReference<Map<String, String>> prevAttrs = new AtomicReference<>();
    room.addListener(
        new RoomListener() {
          @Override
          public void onParticipantMetadataChanged(Room r, Participant p, String prev) {
            prevMetadata.set(prev == null ? "" : prev);
          }

          @Override
          public void onParticipantNameChanged(Room r, Participant p, String prev) {
            prevName.set(prev == null ? "" : prev);
          }

          @Override
          public void onParticipantAttributesChanged(
              Room r, Participant p, Map<String, String> prev) {
            prevAttrs.set(prev);
          }
        });

    room.handleParticipantUpdate(
        LivekitRtc.ParticipantUpdate.newBuilder()
            .addParticipants(
                LivekitModels.ParticipantInfo.newBuilder()
                    .setSid("PA_LOCAL")
                    .setIdentity("local")
                    .setName("Renamed")
                    .setMetadata("meta-1")
                    .putAttributes("a", "1")
                    .build())
            .build());

    assertEquals("", prevMetadata.get());
    assertEquals("Local", prevName.get());
    assertNotNull(prevAttrs.get());
    assertEquals("1", room.getLocalParticipant().getAttributes().get("a"));
    assertEquals("Renamed", room.getLocalParticipant().getName());
  }

  @Test
  void newRemoteParticipantDoesNotFireChangeEvents() {
    List<String> events = new ArrayList<>();
    room.addListener(
        new RoomListener() {
          @Override
          public void onParticipantMetadataChanged(Room r, Participant p, String prev) {
            events.add("metadata");
          }

          @Override
          public void onParticipantConnected(Room r, RemoteParticipant p) {
            events.add("connected");
          }
        });

    room.handleParticipantUpdate(
        LivekitRtc.ParticipantUpdate.newBuilder()
            .addParticipants(
                LivekitModels.ParticipantInfo.newBuilder()
                    .setSid("PA_R1")
                    .setIdentity("remote1")
                    .setMetadata("initial")
                    .build())
            .build());

    assertEquals(List.of("connected"), events);
  }

  @Test
  void publishDataUsesTransport() {
    room.setState(ConnectionState.CONNECTED);
    DataPacket packet =
        DataPacket.builder("hello".getBytes()).kind(DataPacket.Kind.LOSSY).topic("chat").build();

    assertTrue(room.publishData(packet));
    assertNotNull(transport.lastDataPacket);
    assertFalse(transport.lastDataReliable);
    assertEquals("chat", transport.lastDataPacket.getUser().getTopic());
    assertEquals("hello", transport.lastDataPacket.getUser().getPayload().toStringUtf8());
  }

  static class FakeTransport implements RoomTransport {
    LivekitRtc.UpdateParticipantMetadata lastMetadataUpdate;
    LivekitModels.DataPacket lastDataPacket;
    boolean lastDataReliable;

    @Override
    public void sendUpdateMetadata(LivekitRtc.UpdateParticipantMetadata metadata) {
      lastMetadataUpdate = metadata;
    }

    @Override
    public void sendUpdateSubscription(LivekitRtc.UpdateSubscription subscription) {}

    @Override
    public void sendUpdateTrackSettings(LivekitRtc.UpdateTrackSettings settings) {}

    @Override
    public void sendSubscriptionPermission(LivekitRtc.SubscriptionPermission permission) {}

    @Override
    public void sendMuteTrack(String trackSid, boolean muted) {}

    @Override
    public boolean sendDataPacket(LivekitModels.DataPacket packet, boolean reliable) {
      lastDataPacket = packet;
      lastDataReliable = reliable;
      return true;
    }
  }
}
