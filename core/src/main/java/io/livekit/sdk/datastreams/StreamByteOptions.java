package io.livekit.sdk.datastreams;

import java.util.List;
import java.util.Map;

/** Options for sending a byte stream. */
public class StreamByteOptions {
  private String topic = "";
  private String name = "unknown";
  private String mimeType = "application/octet-stream";
  private Long totalLength;
  private List<String> destinationIdentities;
  private Map<String, String> attributes;

  public String getTopic() {
    return topic;
  }

  public StreamByteOptions setTopic(String topic) {
    this.topic = topic;
    return this;
  }

  public String getName() {
    return name;
  }

  /** File or stream name shown to receivers. */
  public StreamByteOptions setName(String name) {
    this.name = name;
    return this;
  }

  public String getMimeType() {
    return mimeType;
  }

  public StreamByteOptions setMimeType(String mimeType) {
    this.mimeType = mimeType;
    return this;
  }

  public Long getTotalLength() {
    return totalLength;
  }

  /** Total stream length in bytes when known ahead of time. */
  public StreamByteOptions setTotalLength(Long totalLength) {
    this.totalLength = totalLength;
    return this;
  }

  public List<String> getDestinationIdentities() {
    return destinationIdentities;
  }

  /** Restrict delivery to specific participants; all participants receive by default. */
  public StreamByteOptions setDestinationIdentities(List<String> destinationIdentities) {
    this.destinationIdentities = destinationIdentities;
    return this;
  }

  public Map<String, String> getAttributes() {
    return attributes;
  }

  /** User-defined attributes carried in the stream header. */
  public StreamByteOptions setAttributes(Map<String, String> attributes) {
    this.attributes = attributes;
    return this;
  }
}
