package io.livekit.sdk;

/** Base class for all room events. */
public abstract class RoomEvent {
  private final Room room;

  protected RoomEvent(Room room) {
    this.room = room;
  }

  public Room getRoom() {
    return room;
  }

  // Connection events
  public static class Connected extends RoomEvent {
    public Connected(Room room) {
      super(room);
    }
  }

  public static class Disconnected extends RoomEvent {
    private final DisconnectReason reason;

    public Disconnected(Room room, DisconnectReason reason) {
      super(room);
      this.reason = reason;
    }

    public DisconnectReason getReason() {
      return reason;
    }
  }

  public static class Reconnecting extends RoomEvent {
    public Reconnecting(Room room) {
      super(room);
    }
  }

  public static class Reconnected extends RoomEvent {
    public Reconnected(Room room) {
      super(room);
    }
  }

  // Participant events
  public static class ParticipantConnected extends RoomEvent {
    private final RemoteParticipant participant;

    public ParticipantConnected(Room room, RemoteParticipant participant) {
      super(room);
      this.participant = participant;
    }

    public RemoteParticipant getParticipant() {
      return participant;
    }
  }

  public static class ParticipantDisconnected extends RoomEvent {
    private final RemoteParticipant participant;

    public ParticipantDisconnected(Room room, RemoteParticipant participant) {
      super(room);
      this.participant = participant;
    }

    public RemoteParticipant getParticipant() {
      return participant;
    }
  }

  public static class ParticipantMetadataChanged extends RoomEvent {
    private final Participant participant;
    private final String prevMetadata;

    public ParticipantMetadataChanged(Room room, Participant participant, String prevMetadata) {
      super(room);
      this.participant = participant;
      this.prevMetadata = prevMetadata;
    }

    public Participant getParticipant() {
      return participant;
    }

    public String getPrevMetadata() {
      return prevMetadata;
    }
  }

  // Track events
  public static class TrackPublished extends RoomEvent {
    private final Participant participant;
    private final TrackPublication publication;

    public TrackPublished(Room room, Participant participant, TrackPublication publication) {
      super(room);
      this.participant = participant;
      this.publication = publication;
    }

    public Participant getParticipant() {
      return participant;
    }

    public TrackPublication getPublication() {
      return publication;
    }
  }

  public static class TrackUnpublished extends RoomEvent {
    private final Participant participant;
    private final TrackPublication publication;

    public TrackUnpublished(Room room, Participant participant, TrackPublication publication) {
      super(room);
      this.participant = participant;
      this.publication = publication;
    }

    public Participant getParticipant() {
      return participant;
    }

    public TrackPublication getPublication() {
      return publication;
    }
  }

  public static class TrackSubscribed extends RoomEvent {
    private final Track track;
    private final TrackPublication publication;
    private final RemoteParticipant participant;

    public TrackSubscribed(
        Room room, Track track, TrackPublication publication, RemoteParticipant participant) {
      super(room);
      this.track = track;
      this.publication = publication;
      this.participant = participant;
    }

    public Track getTrack() {
      return track;
    }

    public TrackPublication getPublication() {
      return publication;
    }

    public RemoteParticipant getParticipant() {
      return participant;
    }
  }

  public static class TrackUnsubscribed extends RoomEvent {
    private final Track track;
    private final TrackPublication publication;
    private final RemoteParticipant participant;

    public TrackUnsubscribed(
        Room room, Track track, TrackPublication publication, RemoteParticipant participant) {
      super(room);
      this.track = track;
      this.publication = publication;
      this.participant = participant;
    }

    public Track getTrack() {
      return track;
    }

    public TrackPublication getPublication() {
      return publication;
    }

    public RemoteParticipant getParticipant() {
      return participant;
    }
  }

  public static class TrackMuted extends RoomEvent {
    private final Participant participant;
    private final TrackPublication publication;

    public TrackMuted(Room room, Participant participant, TrackPublication publication) {
      super(room);
      this.participant = participant;
      this.publication = publication;
    }

    public Participant getParticipant() {
      return participant;
    }

    public TrackPublication getPublication() {
      return publication;
    }
  }

  public static class TrackUnmuted extends RoomEvent {
    private final Participant participant;
    private final TrackPublication publication;

    public TrackUnmuted(Room room, Participant participant, TrackPublication publication) {
      super(room);
      this.participant = participant;
      this.publication = publication;
    }

    public Participant getParticipant() {
      return participant;
    }

    public TrackPublication getPublication() {
      return publication;
    }
  }

  // Data events
  public static class DataReceived extends RoomEvent {
    private final byte[] data;
    private final RemoteParticipant participant;
    private final DataPacket.Kind kind;
    private final String topic;

    public DataReceived(
        Room room, byte[] data, RemoteParticipant participant, DataPacket.Kind kind, String topic) {
      super(room);
      this.data = data;
      this.participant = participant;
      this.kind = kind;
      this.topic = topic;
    }

    public byte[] getData() {
      return data;
    }

    public RemoteParticipant getParticipant() {
      return participant;
    }

    public DataPacket.Kind getKind() {
      return kind;
    }

    public String getTopic() {
      return topic;
    }
  }

  // Audio events
  public static class ActiveSpeakersChanged extends RoomEvent {
    private final java.util.List<Participant> speakers;

    public ActiveSpeakersChanged(Room room, java.util.List<Participant> speakers) {
      super(room);
      this.speakers = speakers;
    }

    public java.util.List<Participant> getSpeakers() {
      return speakers;
    }
  }

  // Connection quality events
  public static class ConnectionQualityChanged extends RoomEvent {
    private final Participant participant;
    private final ConnectionQuality quality;

    public ConnectionQualityChanged(Room room, Participant participant, ConnectionQuality quality) {
      super(room);
      this.participant = participant;
      this.quality = quality;
    }

    public Participant getParticipant() {
      return participant;
    }

    public ConnectionQuality getQuality() {
      return quality;
    }
  }

  // Room metadata changed
  public static class RoomMetadataChanged extends RoomEvent {
    private final String metadata;

    public RoomMetadataChanged(Room room, String metadata) {
      super(room);
      this.metadata = metadata;
    }

    public String getMetadata() {
      return metadata;
    }
  }
}
