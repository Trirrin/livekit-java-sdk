package io.livekit.sdk.datastreams;

import java.util.Collections;
import java.util.Map;

/** Metadata describing a byte data stream (e.g. a file transfer). */
public class ByteStreamInfo {
  private final String streamId;
  private final String topic;
  private final long timestamp;
  private final String mimeType;
  private final String name;
  private final Long totalLength;
  private final Map<String, String> attributes;

  public ByteStreamInfo(
      String streamId,
      String topic,
      long timestamp,
      String mimeType,
      String name,
      Long totalLength,
      Map<String, String> attributes) {
    this.streamId = streamId;
    this.topic = topic;
    this.timestamp = timestamp;
    this.mimeType = mimeType;
    this.name = name;
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

  /** File or stream name. */
  public String getName() {
    return name;
  }

  /** Total length in bytes, or null for streams of unknown size. */
  public Long getTotalLength() {
    return totalLength;
  }

  public Map<String, String> getAttributes() {
    return attributes;
  }
}
