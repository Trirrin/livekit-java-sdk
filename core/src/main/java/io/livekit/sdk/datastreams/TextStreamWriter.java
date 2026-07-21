package io.livekit.sdk.datastreams;

import java.nio.charset.StandardCharsets;
import java.util.List;

/** Writer for an outgoing text stream. Not thread-safe. */
public class TextStreamWriter {
  private final DataStreamManager manager;
  private final TextStreamInfo info;
  private final List<String> destinations;
  private long chunkIndex;
  private boolean closed;

  TextStreamWriter(DataStreamManager manager, TextStreamInfo info, List<String> destinations) {
    this.manager = manager;
    this.info = info;
    this.destinations = destinations;
  }

  public TextStreamInfo getInfo() {
    return info;
  }

  /** Write a piece of text to the stream, split into chunks on code point boundaries. */
  public void write(String text) {
    ensureOpen();
    if (text == null || text.isEmpty()) {
      return;
    }
    StringBuilder current = new StringBuilder();
    int currentBytes = 0;
    int offset = 0;
    while (offset < text.length()) {
      int codePoint = text.codePointAt(offset);
      String piece = new String(Character.toChars(codePoint));
      int pieceBytes = piece.getBytes(StandardCharsets.UTF_8).length;
      if (currentBytes + pieceBytes > DataStreamManager.CHUNK_SIZE && currentBytes > 0) {
        flush(current.toString());
        current.setLength(0);
        currentBytes = 0;
      }
      current.append(piece);
      currentBytes += pieceBytes;
      offset += Character.charCount(codePoint);
    }
    if (currentBytes > 0) {
      flush(current.toString());
    }
  }

  private void flush(String chunk) {
    manager.sendChunk(
        info.getStreamId(), chunkIndex++, chunk.getBytes(StandardCharsets.UTF_8), destinations);
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
