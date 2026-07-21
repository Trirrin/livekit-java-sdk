package io.livekit.sdk.datastreams;

import java.util.Arrays;
import java.util.List;

/** Writer for an outgoing byte stream. Not thread-safe. */
public class ByteStreamWriter {
  private final DataStreamManager manager;
  private final ByteStreamInfo info;
  private final List<String> destinations;
  private long chunkIndex;
  private boolean closed;

  ByteStreamWriter(DataStreamManager manager, ByteStreamInfo info, List<String> destinations) {
    this.manager = manager;
    this.info = info;
    this.destinations = destinations;
  }

  public ByteStreamInfo getInfo() {
    return info;
  }

  /** Write bytes to the stream, split into chunks of at most 15000 bytes. */
  public void write(byte[] data) {
    ensureOpen();
    if (data == null || data.length == 0) {
      return;
    }
    int offset = 0;
    while (offset < data.length) {
      int end = Math.min(offset + DataStreamManager.CHUNK_SIZE, data.length);
      manager.sendChunk(
          info.getStreamId(), chunkIndex++, Arrays.copyOfRange(data, offset, end), destinations);
      offset = end;
    }
  }

  /** Close the stream normally. */
  public void close() {
    close(null);
  }

  /** Close the stream with an error reason reported to receivers. */
  public void close(String reason) {
    if (closed) {
      return;
    }
    closed = true;
    manager.sendTrailer(info.getStreamId(), reason, destinations);
  }

  private void ensureOpen() {
    if (closed) {
      throw new IllegalStateException("Stream is closed");
    }
  }
}
