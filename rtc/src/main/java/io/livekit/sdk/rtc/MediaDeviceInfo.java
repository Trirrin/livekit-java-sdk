package io.livekit.sdk.rtc;

/**
 * Information about a media device (audio input, audio output, or video input).
 */
public class MediaDeviceInfo {

    public enum Kind {
        AUDIO_INPUT,
        AUDIO_OUTPUT,
        VIDEO_INPUT
    }

    private final String deviceId;
    private final String label;
    private final Kind kind;

    public MediaDeviceInfo(String deviceId, String label, Kind kind) {
        this.deviceId = deviceId;
        this.label = label;
        this.kind = kind;
    }

    /**
     * Unique identifier for the device.
     */
    public String getDeviceId() {
        return deviceId;
    }

    /**
     * Human-readable label for the device.
     */
    public String getLabel() {
        return label;
    }

    /**
     * The kind of device.
     */
    public Kind getKind() {
        return kind;
    }

    @Override
    public String toString() {
        return "MediaDeviceInfo{" +
                "deviceId='" + deviceId + '\'' +
                ", label='" + label + '\'' +
                ", kind=" + kind +
                '}';
    }
}
