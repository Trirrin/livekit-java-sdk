package io.livekit.sdk;

import livekit.LivekitRtc;

/** Stream state of a subscribed track. The server pauses streams under congestion. */
public enum TrackStreamState {
  ACTIVE,
  PAUSED,
  UNKNOWN;

  public static TrackStreamState fromProto(LivekitRtc.StreamState proto) {
    switch (proto) {
      case ACTIVE:
        return ACTIVE;
      case PAUSED:
        return PAUSED;
      default:
        return UNKNOWN;
    }
  }
}
