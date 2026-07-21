package io.livekit.sdk;

import io.livekit.sdk.e2ee.E2EEOptions;

/** Options for connecting to a room. */
public class RoomOptions {
  private boolean autoSubscribe = true;
  private boolean adaptiveStream = false;
  private boolean dynacast = false;
  private int reconnectAttempts = 5;
  private long reconnectDelayMs = 1000;
  private boolean e2eeEnabled = false;
  private E2EEOptions e2eeOptions;
  private String preferredVideoCodec;

  public boolean isAutoSubscribe() {
    return autoSubscribe;
  }

  public RoomOptions setAutoSubscribe(boolean autoSubscribe) {
    this.autoSubscribe = autoSubscribe;
    return this;
  }

  public boolean isAdaptiveStream() {
    return adaptiveStream;
  }

  public RoomOptions setAdaptiveStream(boolean adaptiveStream) {
    this.adaptiveStream = adaptiveStream;
    return this;
  }

  public boolean isDynacast() {
    return dynacast;
  }

  public RoomOptions setDynacast(boolean dynacast) {
    this.dynacast = dynacast;
    return this;
  }

  public int getReconnectAttempts() {
    return reconnectAttempts;
  }

  public RoomOptions setReconnectAttempts(int reconnectAttempts) {
    this.reconnectAttempts = reconnectAttempts;
    return this;
  }

  public long getReconnectDelayMs() {
    return reconnectDelayMs;
  }

  public RoomOptions setReconnectDelayMs(long reconnectDelayMs) {
    this.reconnectDelayMs = reconnectDelayMs;
    return this;
  }

  public boolean isE2eeEnabled() {
    return e2eeEnabled;
  }

  public RoomOptions setE2eeEnabled(boolean e2eeEnabled) {
    this.e2eeEnabled = e2eeEnabled;
    return this;
  }

  public String getPreferredVideoCodec() {
    return preferredVideoCodec;
  }

  /** Preferred codec for published video tracks, by name (e.g. "VP8", "H264", "VP9", "AV1"). */
  public RoomOptions setPreferredVideoCodec(String preferredVideoCodec) {
    this.preferredVideoCodec = preferredVideoCodec;
    return this;
  }

  public E2EEOptions getE2eeOptions() {
    return e2eeOptions;
  }

  public RoomOptions setE2eeOptions(E2EEOptions e2eeOptions) {
    this.e2eeOptions = e2eeOptions;
    this.e2eeEnabled = (e2eeOptions != null);
    return this;
  }
}
