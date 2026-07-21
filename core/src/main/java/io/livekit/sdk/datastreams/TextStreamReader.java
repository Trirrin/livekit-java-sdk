package io.livekit.sdk.datastreams;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/** Incremental reader for an incoming text stream. */
public class TextStreamReader {
  private final TextStreamInfo info;
  private final StringBuilder buffer = new StringBuilder();
  private final CompletableFuture<String> result = new CompletableFuture<>();
  private volatile Consumer<String> chunkListener;

  TextStreamReader(TextStreamInfo info) {
    this.info = info;
  }

  public TextStreamInfo getInfo() {
    return info;
  }

  /**
   * Register a listener invoked for incoming chunks. Content buffered before registration is
   * replayed as a single chunk.
   */
  public void onChunk(Consumer<String> listener) {
    synchronized (buffer) {
      this.chunkListener = listener;
      if (buffer.length() > 0) {
        listener.accept(buffer.toString());
      }
    }
  }

  /** Future completed with the full text once the stream is closed by the sender. */
  public CompletableFuture<String> readAll() {
    return result;
  }

  void appendChunk(String chunk) {
    synchronized (buffer) {
      buffer.append(chunk);
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
        result.complete(buffer.toString());
      }
    }
  }
}
