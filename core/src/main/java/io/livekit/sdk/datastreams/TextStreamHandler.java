package io.livekit.sdk.datastreams;

/** Handler invoked when a remote participant opens a text stream on a registered topic. */
@FunctionalInterface
public interface TextStreamHandler {

  /**
   * Called when a text stream is opened. Invoked on a worker thread, so blocking on {@link
   * TextStreamReader#readAll()} is safe.
   *
   * @param reader reader for the incoming stream
   * @param participantIdentity identity of the sending participant
   */
  void onStreamOpened(TextStreamReader reader, String participantIdentity);
}
