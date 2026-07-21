package io.livekit.sdk.datastreams;

import java.util.List;
import java.util.Map;

/** Options for sending a text stream. */
public class StreamTextOptions {
  private String topic = "";
  private List<String> destinationIdentities;
  private Map<String, String> attributes;

  public String getTopic() {
    return topic;
  }

  public StreamTextOptions setTopic(String topic) {
    this.topic = topic;
    return this;
  }

  public List<String> getDestinationIdentities() {
    return destinationIdentities;
  }

  /** Restrict delivery to specific participants; all participants receive by default. */
  public StreamTextOptions setDestinationIdentities(List<String> destinationIdentities) {
    this.destinationIdentities = destinationIdentities;
    return this;
  }

  public Map<String, String> getAttributes() {
    return attributes;
  }

  /** User-defined attributes carried in the stream header. */
  public StreamTextOptions setAttributes(Map<String, String> attributes) {
    this.attributes = attributes;
    return this;
  }
}
