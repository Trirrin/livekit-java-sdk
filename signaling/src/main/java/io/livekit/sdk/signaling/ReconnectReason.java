package io.livekit.sdk.signaling;

/** Reasons for reconnection attempts. Maps to livekit.ReconnectReason from protocol. */
public enum ReconnectReason {
  UNKNOWN,
  SIGNAL_DISCONNECTED,
  PUBLISHER_FAILED,
  SUBSCRIBER_FAILED,
  SWITCH_CANDIDATE;

  public static ReconnectReason fromProto(livekit.LivekitModels.ReconnectReason proto) {
    return switch (proto) {
      case RR_SIGNAL_DISCONNECTED -> SIGNAL_DISCONNECTED;
      case RR_PUBLISHER_FAILED -> PUBLISHER_FAILED;
      case RR_SUBSCRIBER_FAILED -> SUBSCRIBER_FAILED;
      case RR_SWITCH_CANDIDATE -> SWITCH_CANDIDATE;
      default -> UNKNOWN;
    };
  }

  public livekit.LivekitModels.ReconnectReason toProto() {
    return switch (this) {
      case SIGNAL_DISCONNECTED -> livekit.LivekitModels.ReconnectReason.RR_SIGNAL_DISCONNECTED;
      case PUBLISHER_FAILED -> livekit.LivekitModels.ReconnectReason.RR_PUBLISHER_FAILED;
      case SUBSCRIBER_FAILED -> livekit.LivekitModels.ReconnectReason.RR_SUBSCRIBER_FAILED;
      case SWITCH_CANDIDATE -> livekit.LivekitModels.ReconnectReason.RR_SWITCH_CANDIDATE;
      default -> livekit.LivekitModels.ReconnectReason.RR_UNKNOWN;
    };
  }
}
