package io.livekit.sdk;

/**
 * Options for connecting to a room.
 */
public class RoomOptions {
    private boolean autoSubscribe = true;
    private boolean adaptiveStream = false;
    private boolean dynacast = false;
    private int reconnectAttempts = 5;
    private long reconnectDelayMs = 1000;
    private boolean e2eeEnabled = false;

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
}
