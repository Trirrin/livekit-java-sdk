package io.livekit.sdk.signaling;

/**
 * State machine states for signaling connection.
 */
public enum SignalState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    RECONNECTING,
    FAILED
}
