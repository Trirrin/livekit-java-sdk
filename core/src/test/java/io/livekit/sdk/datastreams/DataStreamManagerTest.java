package io.livekit.sdk.datastreams;

import static org.junit.jupiter.api.Assertions.*;

import io.livekit.sdk.RoomTransport;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import livekit.LivekitModels;
import livekit.LivekitRtc;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Tests data stream chunking, reassembly, and handler dispatch over a loopback transport. */
class DataStreamManagerTest {

  private DataStreamManager sender;
  private DataStreamManager receiver;
  private final List<LivekitModels.DataPacket> sentPackets = new ArrayList<>();

  @BeforeEach
  void setUp() {
    RoomTransport loopback =
        new RoomTransport() {
          @Override
          public void sendUpdateMetadata(LivekitRtc.UpdateParticipantMetadata metadata) {}

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
            sentPackets.add(packet);
            switch (packet.getValueCase()) {
              case STREAM_HEADER:
                receiver.handleHeader(packet.getStreamHeader(), "sender");
                break;
              case STREAM_CHUNK:
                receiver.handleChunk(packet.getStreamChunk());
                break;
              case STREAM_TRAILER:
                receiver.handleTrailer(packet.getStreamTrailer());
                break;
              default:
                break;
            }
            return true;
          }
        };
    sender = new DataStreamManager(loopback);
    receiver = new DataStreamManager(loopback);
  }

  private static <T> T awaitNonNull(AtomicReference<T> ref) throws InterruptedException {
    long deadline = System.currentTimeMillis() + 5000;
    while (ref.get() == null && System.currentTimeMillis() < deadline) {
      Thread.sleep(5);
    }
    assertNotNull(ref.get(), "timed out waiting for async handler");
    return ref.get();
  }

  @Test
  void sendTextIsReceivedByRegisteredHandler() throws Exception {
    AtomicReference<TextStreamReader> received = new AtomicReference<>();
    AtomicReference<String> senderIdentity = new AtomicReference<>();
    receiver.registerTextStreamHandler(
        "chat",
        (reader, identity) -> {
          senderIdentity.set(identity);
          received.set(reader);
        });

    TextStreamInfo info = sender.sendText("hello stream", new StreamTextOptions().setTopic("chat"));

    TextStreamReader reader = awaitNonNull(received);
    assertEquals("hello stream", reader.readAll().get(5, TimeUnit.SECONDS));
    assertEquals("sender", senderIdentity.get());
    assertEquals(info.getStreamId(), reader.getInfo().getStreamId());
    assertEquals("chat", reader.getInfo().getTopic());
    assertEquals(12L, reader.getInfo().getTotalLength());
  }

  @Test
  void largeTextIsChunked() throws Exception {
    AtomicReference<TextStreamReader> received = new AtomicReference<>();
    receiver.registerTextStreamHandler("big", (reader, identity) -> received.set(reader));

    String text = "√".repeat(20000); // 3 bytes each in UTF-8, forces multiple chunks
    sender.sendText(text, new StreamTextOptions().setTopic("big"));

    long chunkCount = sentPackets.stream().filter(LivekitModels.DataPacket::hasStreamChunk).count();
    assertTrue(chunkCount > 1, "expected multiple chunks, got " + chunkCount);
    for (LivekitModels.DataPacket packet : sentPackets) {
      if (packet.hasStreamChunk()) {
        assertTrue(packet.getStreamChunk().getContent().size() <= DataStreamManager.CHUNK_SIZE);
      }
    }
    assertEquals(text, awaitNonNull(received).readAll().get(5, TimeUnit.SECONDS));
  }

  @Test
  void sendBytesIsReceivedAndReassembled() throws Exception {
    AtomicReference<ByteStreamReader> received = new AtomicReference<>();
    receiver.registerByteStreamHandler("files", (reader, identity) -> received.set(reader));

    byte[] data = new byte[40000];
    for (int i = 0; i < data.length; i++) {
      data[i] = (byte) (i % 251);
    }
    ByteStreamInfo info =
        sender.sendBytes(data, new StreamByteOptions().setTopic("files").setName("test.bin"));

    ByteStreamReader reader = awaitNonNull(received);
    assertArrayEquals(data, reader.readAll().get(5, TimeUnit.SECONDS));
    assertEquals("test.bin", reader.getInfo().getName());
    assertEquals(40000L, info.getTotalLength());
  }

  @Test
  void unregisteredTopicIsIgnored() {
    // No handler registered; must not throw
    sender.sendText("nobody listening", new StreamTextOptions().setTopic("void"));
  }

  @Test
  void abnormalCloseFailsReader() throws Exception {
    AtomicReference<TextStreamReader> received = new AtomicReference<>();
    receiver.registerTextStreamHandler("err", (reader, identity) -> received.set(reader));

    TextStreamWriter writer = sender.streamText(new StreamTextOptions().setTopic("err"));
    writer.write("partial");
    writer.close("interrupted");

    TextStreamReader reader = awaitNonNull(received);
    ExecutionException e =
        assertThrows(ExecutionException.class, () -> reader.readAll().get(5, TimeUnit.SECONDS));
    assertInstanceOf(DataStreamException.class, e.getCause());
    assertEquals("interrupted", e.getCause().getMessage());
  }

  @Test
  void chunkListenerReceivesAllContent() throws Exception {
    List<String> pieces = new ArrayList<>();
    AtomicReference<TextStreamReader> received = new AtomicReference<>();
    receiver.registerTextStreamHandler(
        "inc",
        (reader, identity) -> {
          reader.onChunk(pieces::add);
          received.set(reader);
        });

    TextStreamWriter writer = sender.streamText(new StreamTextOptions().setTopic("inc"));
    writer.write("one");
    writer.write("two");
    writer.close();

    assertEquals("onetwo", awaitNonNull(received).readAll().get(5, TimeUnit.SECONDS));
    // Chunk boundaries depend on listener registration timing; content must be complete
    assertEquals("onetwo", String.join("", pieces));
  }

  @Test
  void duplicateHandlerRegistrationThrows() {
    receiver.registerTextStreamHandler("dup", (reader, identity) -> {});
    assertThrows(
        IllegalArgumentException.class,
        () -> receiver.registerTextStreamHandler("dup", (reader, identity) -> {}));
  }

  @Test
  void writeAfterCloseThrows() {
    TextStreamWriter writer = sender.streamText(new StreamTextOptions().setTopic("closed"));
    writer.close();
    assertThrows(IllegalStateException.class, () -> writer.write("late"));
  }
}
