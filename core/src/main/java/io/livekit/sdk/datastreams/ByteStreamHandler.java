package io.livekit.sdk.datastreams;

/** Handler invoked when a remote participant opens a byte stream on a registered topic. */
@FunctionalInterface
public interface ByteStreamHandler {

  /**
   * Called when a byte stream is opened. Invoked on a worker thread, so blocking on {@link
   * ByteStreamReader#readAll()} is safe.
   *
   * @param reader reader for the incoming stream
   * @param participantIdentity identity of the sending participant
   */
  void onStreamOpened(ByteStreamReader reader, String participantIdentity);
}
