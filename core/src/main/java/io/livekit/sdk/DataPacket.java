package io.livekit.sdk;

import java.util.List;

/** Represents a data packet that can be sent to other participants. */
public class DataPacket {

  public enum Kind {
    RELIABLE,
    LOSSY
  }

  private final byte[] data;
  private final Kind kind;
  private final String topic;
  private final List<String> destinationIdentities;

  private DataPacket(Builder builder) {
    this.data = builder.data;
    this.kind = builder.kind;
    this.topic = builder.topic;
    this.destinationIdentities = builder.destinationIdentities;
  }

  public byte[] getData() {
    return data;
  }

  public Kind getKind() {
    return kind;
  }

  public String getTopic() {
    return topic;
  }

  public List<String> getDestinationIdentities() {
    return destinationIdentities;
  }

  public static Builder builder(byte[] data) {
    return new Builder(data);
  }

  public static class Builder {
    private final byte[] data;
    private Kind kind = Kind.RELIABLE;
    private String topic;
    private List<String> destinationIdentities;

    private Builder(byte[] data) {
      this.data = data;
    }

    public Builder kind(Kind kind) {
      this.kind = kind;
      return this;
    }

    public Builder topic(String topic) {
      this.topic = topic;
      return this;
    }

    public Builder destinationIdentities(List<String> identities) {
      this.destinationIdentities = identities;
      return this;
    }

    public DataPacket build() {
      return new DataPacket(this);
    }
  }
}
