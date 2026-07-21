package io.livekit.sdk;

import livekit.LivekitModels;

/** Video quality preference for a subscribed track. */
public enum VideoQuality {
  LOW,
  MEDIUM,
  HIGH,
  OFF;

  public LivekitModels.VideoQuality toProto() {
    switch (this) {
      case LOW:
        return LivekitModels.VideoQuality.LOW;
      case MEDIUM:
        return LivekitModels.VideoQuality.MEDIUM;
      case HIGH:
        return LivekitModels.VideoQuality.HIGH;
      case OFF:
        return LivekitModels.VideoQuality.OFF;
      default:
        return LivekitModels.VideoQuality.HIGH;
    }
  }

  public static VideoQuality fromProto(LivekitModels.VideoQuality proto) {
    switch (proto) {
      case LOW:
        return LOW;
      case MEDIUM:
        return MEDIUM;
      case HIGH:
        return HIGH;
      case OFF:
        return OFF;
      default:
        return HIGH;
    }
  }
}
