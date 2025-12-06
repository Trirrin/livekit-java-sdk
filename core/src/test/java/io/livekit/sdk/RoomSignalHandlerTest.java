package io.livekit.sdk;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import livekit.LivekitModels;
import livekit.LivekitRtc;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RoomSignalHandlerTest {
  private Room room;
  private RoomSignalHandler handler;

  @BeforeEach
  void setUp() {
    room = new Room();
    handler = room.getSignalHandler();
  }

  @Test
  void testJoinResponseCreatesLocalParticipant() {
    LivekitRtc.JoinResponse response =
        LivekitRtc.JoinResponse.newBuilder()
            .setRoom(
                LivekitModels.Room.newBuilder()
                    .setSid("RM_123")
                    .setName("test-room")
                    .setMetadata("{\"description\":\"Test\"}")
                    .build())
            .setParticipant(
                LivekitModels.ParticipantInfo.newBuilder()
                    .setSid("PA_local")
                    .setIdentity("local-user")
                    .setName("Local User")
                    .build())
            .build();

    AtomicBoolean connectedCalled = new AtomicBoolean(false);
    room.addListener(
        new RoomListener() {
          @Override
          public void onConnected(Room r) {
            connectedCalled.set(true);
          }
        });

    handler.onJoinResponse(response);

    assertEquals("RM_123", room.getSid());
    assertEquals("test-room", room.getName());
    assertEquals("{\"description\":\"Test\"}", room.getMetadata());
    assertEquals(ConnectionState.CONNECTED, room.getState());
    assertNotNull(room.getLocalParticipant());
    assertEquals("PA_local", room.getLocalParticipant().getSid());
    assertEquals("local-user", room.getLocalParticipant().getIdentity());
    assertTrue(connectedCalled.get());
  }

  @Test
  void testJoinResponseWithOtherParticipants() {
    LivekitRtc.JoinResponse response =
        LivekitRtc.JoinResponse.newBuilder()
            .setRoom(LivekitModels.Room.newBuilder().setSid("RM_123").setName("test-room").build())
            .setParticipant(
                LivekitModels.ParticipantInfo.newBuilder()
                    .setSid("PA_local")
                    .setIdentity("local-user")
                    .build())
            .addOtherParticipants(
                LivekitModels.ParticipantInfo.newBuilder()
                    .setSid("PA_remote1")
                    .setIdentity("remote-user-1")
                    .setName("Remote 1")
                    .build())
            .addOtherParticipants(
                LivekitModels.ParticipantInfo.newBuilder()
                    .setSid("PA_remote2")
                    .setIdentity("remote-user-2")
                    .setName("Remote 2")
                    .build())
            .build();

    handler.onJoinResponse(response);

    assertEquals(2, room.getRemoteParticipants().size());
    assertNotNull(room.getRemoteParticipant("remote-user-1"));
    assertNotNull(room.getRemoteParticipant("remote-user-2"));
    assertEquals("Remote 1", room.getRemoteParticipant("remote-user-1").getName());
  }

  @Test
  void testParticipantUpdateAddsNewParticipant() {
    // First join
    LivekitRtc.JoinResponse joinResponse =
        LivekitRtc.JoinResponse.newBuilder()
            .setRoom(LivekitModels.Room.newBuilder().setSid("RM_123").setName("test").build())
            .setParticipant(
                LivekitModels.ParticipantInfo.newBuilder()
                    .setSid("PA_local")
                    .setIdentity("local")
                    .build())
            .build();
    handler.onJoinResponse(joinResponse);

    AtomicReference<RemoteParticipant> connectedParticipant = new AtomicReference<>();
    room.addListener(
        new RoomListener() {
          @Override
          public void onParticipantConnected(Room r, RemoteParticipant p) {
            connectedParticipant.set(p);
          }
        });

    // New participant joins
    LivekitRtc.ParticipantUpdate update =
        LivekitRtc.ParticipantUpdate.newBuilder()
            .addParticipants(
                LivekitModels.ParticipantInfo.newBuilder()
                    .setSid("PA_new")
                    .setIdentity("new-user")
                    .setName("New User")
                    .setState(LivekitModels.ParticipantInfo.State.ACTIVE)
                    .build())
            .build();

    handler.onParticipantUpdate(update);

    assertEquals(1, room.getRemoteParticipants().size());
    assertNotNull(room.getRemoteParticipant("new-user"));
    assertNotNull(connectedParticipant.get());
    assertEquals("new-user", connectedParticipant.get().getIdentity());
  }

  @Test
  void testParticipantUpdateRemovesDisconnectedParticipant() {
    // Setup with existing participant
    LivekitRtc.JoinResponse joinResponse =
        LivekitRtc.JoinResponse.newBuilder()
            .setRoom(LivekitModels.Room.newBuilder().setSid("RM_123").setName("test").build())
            .setParticipant(
                LivekitModels.ParticipantInfo.newBuilder()
                    .setSid("PA_local")
                    .setIdentity("local")
                    .build())
            .addOtherParticipants(
                LivekitModels.ParticipantInfo.newBuilder()
                    .setSid("PA_remote")
                    .setIdentity("remote-user")
                    .build())
            .build();
    handler.onJoinResponse(joinResponse);
    assertEquals(1, room.getRemoteParticipants().size());

    AtomicReference<RemoteParticipant> disconnectedParticipant = new AtomicReference<>();
    room.addListener(
        new RoomListener() {
          @Override
          public void onParticipantDisconnected(Room r, RemoteParticipant p) {
            disconnectedParticipant.set(p);
          }
        });

    // Participant disconnects
    LivekitRtc.ParticipantUpdate update =
        LivekitRtc.ParticipantUpdate.newBuilder()
            .addParticipants(
                LivekitModels.ParticipantInfo.newBuilder()
                    .setSid("PA_remote")
                    .setIdentity("remote-user")
                    .setState(LivekitModels.ParticipantInfo.State.DISCONNECTED)
                    .build())
            .build();

    handler.onParticipantUpdate(update);

    assertEquals(0, room.getRemoteParticipants().size());
    assertNotNull(disconnectedParticipant.get());
  }

  @Test
  void testRoomUpdateChangesMetadata() {
    // Setup
    LivekitRtc.JoinResponse joinResponse =
        LivekitRtc.JoinResponse.newBuilder()
            .setRoom(
                LivekitModels.Room.newBuilder()
                    .setSid("RM_123")
                    .setName("test")
                    .setMetadata("old")
                    .build())
            .setParticipant(
                LivekitModels.ParticipantInfo.newBuilder()
                    .setSid("PA_local")
                    .setIdentity("local")
                    .build())
            .build();
    handler.onJoinResponse(joinResponse);

    AtomicReference<String> newMetadata = new AtomicReference<>();
    room.addListener(
        new RoomListener() {
          @Override
          public void onRoomMetadataChanged(Room r, String metadata) {
            newMetadata.set(metadata);
          }
        });

    // Room update
    LivekitRtc.RoomUpdate update =
        LivekitRtc.RoomUpdate.newBuilder()
            .setRoom(
                LivekitModels.Room.newBuilder()
                    .setSid("RM_123")
                    .setName("test")
                    .setMetadata("new-metadata")
                    .build())
            .build();

    handler.onRoomUpdate(update);

    assertEquals("new-metadata", room.getMetadata());
    assertEquals("new-metadata", newMetadata.get());
  }

  @Test
  void testLeaveDisconnectsRoom() {
    // Setup
    LivekitRtc.JoinResponse joinResponse =
        LivekitRtc.JoinResponse.newBuilder()
            .setRoom(LivekitModels.Room.newBuilder().setSid("RM_123").setName("test").build())
            .setParticipant(
                LivekitModels.ParticipantInfo.newBuilder()
                    .setSid("PA_local")
                    .setIdentity("local")
                    .build())
            .build();
    handler.onJoinResponse(joinResponse);

    AtomicReference<DisconnectReason> disconnectReason = new AtomicReference<>();
    room.addListener(
        new RoomListener() {
          @Override
          public void onDisconnected(Room r, DisconnectReason reason) {
            disconnectReason.set(reason);
          }
        });

    LivekitRtc.LeaveRequest leave =
        LivekitRtc.LeaveRequest.newBuilder()
            .setReason(LivekitModels.DisconnectReason.SERVER_SHUTDOWN)
            .build();

    handler.onLeave(leave);

    assertEquals(ConnectionState.DISCONNECTED, room.getState());
    assertEquals(DisconnectReason.SERVER_SHUTDOWN, disconnectReason.get());
  }

  @Test
  void testReconnectingAndReconnected() {
    // Setup
    LivekitRtc.JoinResponse joinResponse =
        LivekitRtc.JoinResponse.newBuilder()
            .setRoom(LivekitModels.Room.newBuilder().setSid("RM_123").setName("test").build())
            .setParticipant(
                LivekitModels.ParticipantInfo.newBuilder()
                    .setSid("PA_local")
                    .setIdentity("local")
                    .build())
            .build();
    handler.onJoinResponse(joinResponse);

    AtomicBoolean reconnectingCalled = new AtomicBoolean(false);
    AtomicBoolean reconnectedCalled = new AtomicBoolean(false);
    room.addListener(
        new RoomListener() {
          @Override
          public void onReconnecting(Room r) {
            reconnectingCalled.set(true);
          }

          @Override
          public void onReconnected(Room r) {
            reconnectedCalled.set(true);
          }
        });

    handler.onStateChanged("RECONNECTING");
    assertTrue(reconnectingCalled.get());
    assertEquals(ConnectionState.RECONNECTING, room.getState());

    handler.onReconnectResponse(LivekitRtc.ReconnectResponse.newBuilder().build());
    assertTrue(reconnectedCalled.get());
    assertEquals(ConnectionState.CONNECTED, room.getState());
  }
}
