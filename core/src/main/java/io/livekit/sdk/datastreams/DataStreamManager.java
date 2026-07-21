package io.livekit.sdk.datastreams;

import io.livekit.sdk.RoomTransport;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import livekit.LivekitModels;

/**
 * Implements LiveKit data streams over reliable data channels: chunked sending of text and byte
 * streams, and reassembly plus topic-based handler dispatch for incoming streams.
 */
public class DataStreamManager {

  /** Maximum chunk content size in bytes, matching official SDKs. */
  public static final int CHUNK_SIZE = 15000;

  private final RoomTransport transport;
  private final Map<String, TextStreamHandler> textHandlers = new ConcurrentHashMap<>();
  private final Map<String, ByteStreamHandler> byteHandlers = new ConcurrentHashMap<>();
  private final Map<String, TextStreamReader> activeTextStreams = new ConcurrentHashMap<>();
  private final Map<String, ByteStreamReader> activeByteStreams = new ConcurrentHashMap<>();
  private final ExecutorService handlerExecutor;

  public DataStreamManager(RoomTransport transport) {
    this.transport = transport;
    this.handlerExecutor =
        Executors.newCachedThreadPool(
            runnable -> {
              Thread thread = new Thread(runnable, "lk-datastream-handler");
              thread.setDaemon(true);
              return thread;
            });
  }

  // Handler registration

  /** Register a handler for incoming text streams on a topic. Throws if already registered. */
  public void registerTextStreamHandler(String topic, TextStreamHandler handler) {
    if (textHandlers.putIfAbsent(topic, handler) != null) {
      throw new IllegalArgumentException("Text stream handler already registered: " + topic);
    }
  }

  public void unregisterTextStreamHandler(String topic) {
    textHandlers.remove(topic);
  }

  /** Register a handler for incoming byte streams on a topic. Throws if already registered. */
  public void registerByteStreamHandler(String topic, ByteStreamHandler handler) {
    if (byteHandlers.putIfAbsent(topic, handler) != null) {
      throw new IllegalArgumentException("Byte stream handler already registered: " + topic);
    }
  }

  public void unregisterByteStreamHandler(String topic) {
    byteHandlers.remove(topic);
  }

  // Outgoing streams

  /** Send a complete text in one stream. */
  public TextStreamInfo sendText(String text, StreamTextOptions options) {
    TextStreamWriter writer = streamText(options, (long) utf8Length(text));
    writer.write(text);
    writer.close();
    return writer.getInfo();
  }

  /** Open an incremental text stream of unknown total size. */
  public TextStreamWriter streamText(StreamTextOptions options) {
    return streamText(options, null);
  }

  private TextStreamWriter streamText(StreamTextOptions options, Long totalLength) {
    String streamId = UUID.randomUUID().toString();
    long timestamp = System.currentTimeMillis();
    TextStreamInfo info =
        new TextStreamInfo(
            streamId,
            options.getTopic(),
            timestamp,
            "text/plain",
            totalLength,
            options.getAttributes());

    LivekitModels.DataStream.Header.Builder header =
        LivekitModels.DataStream.Header.newBuilder()
            .setStreamId(streamId)
            .setTimestamp(timestamp)
            .setTopic(options.getTopic())
            .setMimeType("text/plain")
            .setTextHeader(
                LivekitModels.DataStream.TextHeader.newBuilder()
                    .setOperationType(LivekitModels.DataStream.OperationType.CREATE));
    if (totalLength != null) {
      header.setTotalLength(totalLength);
    }
    if (options.getAttributes() != null) {
      header.putAllAttributes(options.getAttributes());
    }
    sendHeader(header.build(), options.getDestinationIdentities());

    return new TextStreamWriter(this, info, options.getDestinationIdentities());
  }

  /** Send a complete byte array in one stream. */
  public ByteStreamInfo sendBytes(byte[] data, StreamByteOptions options) {
    options.setTotalLength((long) data.length);
    ByteStreamWriter writer = streamBytes(options);
    writer.write(data);
    writer.close();
    return writer.getInfo();
  }

  /** Open an incremental byte stream. */
  public ByteStreamWriter streamBytes(StreamByteOptions options) {
    String streamId = UUID.randomUUID().toString();
    long timestamp = System.currentTimeMillis();
    ByteStreamInfo info =
        new ByteStreamInfo(
            streamId,
            options.getTopic(),
            timestamp,
            options.getMimeType(),
            options.getName(),
            options.getTotalLength(),
            options.getAttributes());

    LivekitModels.DataStream.Header.Builder header =
        LivekitModels.DataStream.Header.newBuilder()
            .setStreamId(streamId)
            .setTimestamp(timestamp)
            .setTopic(options.getTopic())
            .setMimeType(options.getMimeType())
            .setByteHeader(
                LivekitModels.DataStream.ByteHeader.newBuilder().setName(options.getName()));
    if (options.getTotalLength() != null) {
      header.setTotalLength(options.getTotalLength());
    }
    if (options.getAttributes() != null) {
      header.putAllAttributes(options.getAttributes());
    }
    sendHeader(header.build(), options.getDestinationIdentities());

    return new ByteStreamWriter(this, info, options.getDestinationIdentities());
  }

  // Wire-level sending, used by writers

  private void sendHeader(
      LivekitModels.DataStream.Header header, List<String> destinationIdentities) {
    LivekitModels.DataPacket.Builder packet =
        LivekitModels.DataPacket.newBuilder().setStreamHeader(header);
    addDestinations(packet, destinationIdentities);
    send(packet.build());
  }

  void sendChunk(String streamId, long chunkIndex, byte[] content, List<String> destinations) {
    LivekitModels.DataPacket.Builder packet =
        LivekitModels.DataPacket.newBuilder()
            .setStreamChunk(
                LivekitModels.DataStream.Chunk.newBuilder()
                    .setStreamId(streamId)
                    .setChunkIndex(chunkIndex)
                    .setContent(com.google.protobuf.ByteString.copyFrom(content)));
    addDestinations(packet, destinations);
    send(packet.build());
  }

  void sendTrailer(String streamId, String reason, List<String> destinations) {
    LivekitModels.DataPacket.Builder packet =
        LivekitModels.DataPacket.newBuilder()
            .setStreamTrailer(
                LivekitModels.DataStream.Trailer.newBuilder()
                    .setStreamId(streamId)
                    .setReason(reason == null ? "" : reason));
    addDestinations(packet, destinations);
    send(packet.build());
  }

  private void addDestinations(LivekitModels.DataPacket.Builder packet, List<String> identities) {
    if (identities != null) {
      packet.addAllDestinationIdentities(identities);
    }
  }

  private void send(LivekitModels.DataPacket packet) {
    if (!transport.sendDataPacket(packet, true)) {
      throw new DataStreamException("Failed to send data stream packet: channel not open");
    }
  }

  // Incoming streams

  /** Handle an incoming stream header. */
  public void handleHeader(LivekitModels.DataStream.Header header, String participantIdentity) {
    String topic = header.getTopic();
    Long totalLength = header.hasTotalLength() ? header.getTotalLength() : null;

    if (header.hasTextHeader()) {
      TextStreamHandler handler = textHandlers.get(topic);
      if (handler == null) {
        return;
      }
      TextStreamInfo info =
          new TextStreamInfo(
              header.getStreamId(),
              topic,
              header.getTimestamp(),
              header.getMimeType(),
              totalLength,
              header.getAttributesMap());
      TextStreamReader reader = new TextStreamReader(info);
      activeTextStreams.put(header.getStreamId(), reader);
      handlerExecutor.execute(() -> handler.onStreamOpened(reader, participantIdentity));
    } else if (header.hasByteHeader()) {
      ByteStreamHandler handler = byteHandlers.get(topic);
      if (handler == null) {
        return;
      }
      ByteStreamInfo info =
          new ByteStreamInfo(
              header.getStreamId(),
              topic,
              header.getTimestamp(),
              header.getMimeType(),
              header.getByteHeader().getName(),
              totalLength,
              header.getAttributesMap());
      ByteStreamReader reader = new ByteStreamReader(info);
      activeByteStreams.put(header.getStreamId(), reader);
      handlerExecutor.execute(() -> handler.onStreamOpened(reader, participantIdentity));
    }
  }

  /** Handle an incoming stream chunk. */
  public void handleChunk(LivekitModels.DataStream.Chunk chunk) {
    TextStreamReader textReader = activeTextStreams.get(chunk.getStreamId());
    if (textReader != null) {
      textReader.appendChunk(chunk.getContent().toStringUtf8());
      return;
    }
    ByteStreamReader byteReader = activeByteStreams.get(chunk.getStreamId());
    if (byteReader != null) {
      byteReader.appendChunk(chunk.getContent().toByteArray());
    }
  }

  /** Handle an incoming stream trailer, completing the stream. */
  public void handleTrailer(LivekitModels.DataStream.Trailer trailer) {
    TextStreamReader textReader = activeTextStreams.remove(trailer.getStreamId());
    if (textReader != null) {
      textReader.complete(trailer.getReason());
      return;
    }
    ByteStreamReader byteReader = activeByteStreams.remove(trailer.getStreamId());
    if (byteReader != null) {
      byteReader.complete(trailer.getReason());
    }
  }

  static int utf8Length(String value) {
    return value == null ? 0 : value.getBytes(StandardCharsets.UTF_8).length;
  }
}
