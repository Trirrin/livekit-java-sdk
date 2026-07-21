package io.livekit.sdk.datastreams;

import java.util.Collections;
import java.util.Map;

/** Metadata describing a text data stream. */
public class TextStreamInfo {
  private final String streamId;
  private final String topic;
  private final long timestamp;
  private final String mimeType;
  private final Long totalLength;
  private final Map<String, String> attributes;

  public TextStreamInfo(
      String streamId,
      String topic,
      long timestamp,
      String mimeType,
      Long totalLength,
      Map<String, String> attributes) {
    this.streamId = streamId;
    this.topic = topic;
    this.timestamp = timestamp;
    this.mimeType = mimeType;
    this.totalLength = totalLength;
    this.attributes = attributes == null ? Collections.emptyMap() : Map.copyOf(attributes);
  }

  public String getStreamId() {
    return streamId;
  }

  public String getTopic() {
    return topic;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public String getMimeType() {
    return mimeType;
  }

  /** Total length in bytes, or null for streams of unknown size. */
  public Long getTotalLength() {
    return totalLength;
  }

  public Map<String, String> getAttributes() {
    return attributes;
  }
}
