package io.livekit.sdk;

import java.util.Map;
import livekit.LivekitModels;
import livekit.LivekitRtc;

/** Utility class to convert between protobuf types and SDK types. */
public final class ProtoConverter {

  private ProtoConverter() {}

  public static TrackType fromProto(LivekitModels.TrackType protoType) {
    switch (protoType) {
      case AUDIO:
        return TrackType.AUDIO;
      case VIDEO:
        return TrackType.VIDEO;
      case DATA:
        return TrackType.DATA;
      default:
        return TrackType.AUDIO;
    }
  }

  public static LivekitModels.TrackType toProto(TrackType type) {
    switch (type) {
      case AUDIO:
        return LivekitModels.TrackType.AUDIO;
      case VIDEO:
        return LivekitModels.TrackType.VIDEO;
      case DATA:
        return LivekitModels.TrackType.DATA;
      default:
        return LivekitModels.TrackType.AUDIO;
    }
  }

  public static TrackSource fromProto(LivekitModels.TrackSource protoSource) {
    switch (protoSource) {
      case CAMERA:
        return TrackSource.CAMERA;
      case MICROPHONE:
        return TrackSource.MICROPHONE;
      case SCREEN_SHARE:
        return TrackSource.SCREEN_SHARE;
      case SCREEN_SHARE_AUDIO:
        return TrackSource.SCREEN_SHARE_AUDIO;
      default:
        return TrackSource.UNKNOWN;
    }
  }

  public static LivekitModels.TrackSource toProto(TrackSource source) {
    switch (source) {
      case CAMERA:
        return LivekitModels.TrackSource.CAMERA;
      case MICROPHONE:
        return LivekitModels.TrackSource.MICROPHONE;
      case SCREEN_SHARE:
        return LivekitModels.TrackSource.SCREEN_SHARE;
      case SCREEN_SHARE_AUDIO:
        return LivekitModels.TrackSource.SCREEN_SHARE_AUDIO;
      default:
        return LivekitModels.TrackSource.UNKNOWN;
    }
  }

  public static ConnectionQuality fromProto(LivekitModels.ConnectionQuality protoQuality) {
    switch (protoQuality) {
      case POOR:
        return ConnectionQuality.POOR;
      case GOOD:
        return ConnectionQuality.GOOD;
      case EXCELLENT:
        return ConnectionQuality.EXCELLENT;
      case LOST:
        return ConnectionQuality.LOST;
      default:
        return ConnectionQuality.UNKNOWN;
    }
  }

  public static DisconnectReason fromProto(LivekitModels.DisconnectReason protoReason) {
    switch (protoReason) {
      case CLIENT_INITIATED:
        return DisconnectReason.CLIENT_INITIATED;
      case DUPLICATE_IDENTITY:
        return DisconnectReason.DUPLICATE_IDENTITY;
      case SERVER_SHUTDOWN:
        return DisconnectReason.SERVER_SHUTDOWN;
      case PARTICIPANT_REMOVED:
        return DisconnectReason.PARTICIPANT_REMOVED;
      case ROOM_DELETED:
        return DisconnectReason.ROOM_DELETED;
      case STATE_MISMATCH:
        return DisconnectReason.STATE_MISMATCH;
      case JOIN_FAILURE:
        return DisconnectReason.JOIN_FAILURE;
      case MIGRATION:
        return DisconnectReason.MIGRATION;
      case SIGNAL_CLOSE:
        return DisconnectReason.SIGNAL_CLOSE;
      case ROOM_CLOSED:
        return DisconnectReason.ROOM_CLOSED;
      case USER_UNAVAILABLE:
        return DisconnectReason.USER_UNAVAILABLE;
      case USER_REJECTED:
        return DisconnectReason.USER_REJECTED;
      case SIP_TRUNK_FAILURE:
        return DisconnectReason.SIP_TRUNK_FAILURE;
      default:
        return DisconnectReason.UNKNOWN;
    }
  }

  public static LocalParticipant localParticipantFromProto(LivekitModels.ParticipantInfo info) {
    LocalParticipant participant = new LocalParticipant(info.getSid(), info.getIdentity());
    updateParticipantFromProto(participant, info);
    return participant;
  }

  public static RemoteParticipant remoteParticipantFromProto(LivekitModels.ParticipantInfo info) {
    RemoteParticipant participant = new RemoteParticipant(info.getSid(), info.getIdentity());
    updateParticipantFromProto(participant, info);
    return participant;
  }

  public static void updateParticipantFromProto(
      Participant participant, LivekitModels.ParticipantInfo info) {
    participant.setName(info.getName());
    participant.setMetadata(info.getMetadata());
    if (info.getAttributesCount() > 0) {
      participant.setAttributes(info.getAttributesMap());
    }
  }

  public static TrackPublication trackPublicationFromProto(LivekitModels.TrackInfo info) {
    TrackPublication publication =
        new TrackPublication(info.getSid(), info.getName(), fromProto(info.getType()));
    publication.setSource(fromProto(info.getSource()));
    publication.setMuted(info.getMuted());
    publication.setMimeType(info.getMimeType());
    return publication;
  }

  public static void updateTrackPublicationFromProto(
      TrackPublication publication, LivekitModels.TrackInfo info) {
    publication.setSource(fromProto(info.getSource()));
    publication.setMuted(info.getMuted());
    publication.setMimeType(info.getMimeType());
  }

  public static LivekitRtc.UpdateParticipantMetadata buildMetadataUpdate(
      String metadata, String name, Map<String, String> attributes) {
    LivekitRtc.UpdateParticipantMetadata.Builder builder =
        LivekitRtc.UpdateParticipantMetadata.newBuilder();
    if (metadata != null) {
      builder.setMetadata(metadata);
    }
    if (name != null) {
      builder.setName(name);
    }
    if (attributes != null) {
      builder.putAllAttributes(attributes);
    }
    return builder.build();
  }
}
