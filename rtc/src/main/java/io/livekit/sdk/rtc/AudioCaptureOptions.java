package io.livekit.sdk.rtc;

import dev.onvoid.webrtc.media.audio.AudioOptions;

/**
 * Audio capture processing options. Defaults match the official LiveKit client SDKs: echo
 * cancellation, noise suppression, and auto gain control are enabled.
 */
public class AudioCaptureOptions {
  private boolean echoCancellation = true;
  private boolean noiseSuppression = true;
  private boolean autoGainControl = true;
  private boolean highPassFilter = true;

  public boolean isEchoCancellation() {
    return echoCancellation;
  }

  public AudioCaptureOptions setEchoCancellation(boolean echoCancellation) {
    this.echoCancellation = echoCancellation;
    return this;
  }

  public boolean isNoiseSuppression() {
    return noiseSuppression;
  }

  public AudioCaptureOptions setNoiseSuppression(boolean noiseSuppression) {
    this.noiseSuppression = noiseSuppression;
    return this;
  }

  public boolean isAutoGainControl() {
    return autoGainControl;
  }

  public AudioCaptureOptions setAutoGainControl(boolean autoGainControl) {
    this.autoGainControl = autoGainControl;
    return this;
  }

  public boolean isHighPassFilter() {
    return highPassFilter;
  }

  public AudioCaptureOptions setHighPassFilter(boolean highPassFilter) {
    this.highPassFilter = highPassFilter;
    return this;
  }

  AudioOptions toNative() {
    AudioOptions options = new AudioOptions();
    options.echoCancellation = echoCancellation;
    options.noiseSuppression = noiseSuppression;
    options.autoGainControl = autoGainControl;
    options.highpassFilter = highPassFilter;
    return options;
  }
}
