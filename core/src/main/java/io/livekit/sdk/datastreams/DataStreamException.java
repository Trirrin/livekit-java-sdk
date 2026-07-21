package io.livekit.sdk.datastreams;

/** Raised when a data stream fails or is closed abnormally by the sender. */
public class DataStreamException extends RuntimeException {
  public DataStreamException(String message) {
    super(message);
  }
}
