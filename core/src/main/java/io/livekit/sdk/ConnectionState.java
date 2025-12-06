package io.livekit.sdk;

/**
 * Connection state of the room.
 * Aligns with Web/Android SDK naming.
 */
public enum ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    RECONNECTING
}
