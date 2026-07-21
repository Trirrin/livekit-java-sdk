package io.livekit.sdk.datastreams;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/** Incremental reader for an incoming byte stream. */
public class ByteStreamReader {
  private final ByteStreamInfo info;
  private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
  private final CompletableFuture<byte[]> result = new CompletableFuture<>();
  private volatile Consumer<byte[]> chunkListener;

  ByteStreamReader(ByteStreamInfo info) {
    this.info = info;
  }

  public ByteStreamInfo getInfo() {
    return info;
  }

  /**
   * Register a listener invoked for incoming chunks. Content buffered before registration is
   * replayed as a single chunk.
   */
  public void onChunk(Consumer<byte[]> listener) {
    synchronized (buffer) {
      this.chunkListener = listener;
      if (buffer.size() > 0) {
        listener.accept(buffer.toByteArray());
      }
    }
  }

  /** Future completed with the full content once the stream is closed by the sender. */
  public CompletableFuture<byte[]> readAll() {
    return result;
  }

  void appendChunk(byte[] chunk) {
    synchronized (buffer) {
      buffer.writeBytes(chunk);
      if (chunkListener != null) {
        chunkListener.accept(chunk);
      }
    }
  }

  void complete(String errorReason) {
    if (errorReason != null && !errorReason.isEmpty()) {
      result.completeExceptionally(new DataStreamException(errorReason));
    } else {
      synchronized (buffer) {
        result.complete(buffer.toByteArray());
      }
    }
  }
}
