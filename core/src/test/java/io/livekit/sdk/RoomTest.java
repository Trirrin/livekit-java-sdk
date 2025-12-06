package io.livekit.sdk;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RoomTest {
  private Room room;

  @BeforeEach
  void setUp() {
    room = new Room();
  }

  @Test
  void testInitialState() {
    assertEquals(ConnectionState.DISCONNECTED, room.getState());
    assertNull(room.getLocalParticipant());
    assertTrue(room.getRemoteParticipants().isEmpty());
  }

  @Test
  void testAddRemoveListener() {
    RoomListener listener = new RoomListener() {};
    room.addListener(listener);
    room.removeListener(listener);
  }

  @Test
  void testConnectWhenNotDisconnected() {
    room.setState(ConnectionState.CONNECTING);
    assertThrows(IllegalStateException.class, () -> room.connect("wss://test", "token"));
  }

  @Test
  void testRoomOptions() {
    RoomOptions options = new RoomOptions().setAutoSubscribe(true).setReconnectAttempts(3);

    Room roomWithOptions = new Room(options);
    assertEquals(options, roomWithOptions.getOptions());
    assertTrue(options.isAutoSubscribe());
    assertEquals(3, options.getReconnectAttempts());
  }
}
