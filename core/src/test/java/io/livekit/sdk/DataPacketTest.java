package io.livekit.sdk;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

class DataPacketTest {

  @Test
  void testDataPacketBuilder() {
    byte[] data = "hello world".getBytes();
    DataPacket packet =
        DataPacket.builder(data)
            .kind(DataPacket.Kind.RELIABLE)
            .topic("chat")
            .destinationIdentities(Arrays.asList("user1", "user2"))
            .build();

    assertArrayEquals(data, packet.getData());
    assertEquals(DataPacket.Kind.RELIABLE, packet.getKind());
    assertEquals("chat", packet.getTopic());
    assertEquals(2, packet.getDestinationIdentities().size());
  }

  @Test
  void testDataPacketDefaults() {
    byte[] data = new byte[] {1, 2, 3};
    DataPacket packet = DataPacket.builder(data).build();

    assertEquals(DataPacket.Kind.RELIABLE, packet.getKind());
    assertNull(packet.getTopic());
    assertNull(packet.getDestinationIdentities());
  }

  @Test
  void testLossyKind() {
    byte[] data = "audio".getBytes();
    DataPacket packet = DataPacket.builder(data).kind(DataPacket.Kind.LOSSY).build();

    assertEquals(DataPacket.Kind.LOSSY, packet.getKind());
  }
}
