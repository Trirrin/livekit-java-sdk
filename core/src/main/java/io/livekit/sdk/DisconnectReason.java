package io.livekit.sdk;

/**
 * Reason for disconnection from the room.
 */
public enum DisconnectReason {
    UNKNOWN,
    CLIENT_INITIATED,
    DUPLICATE_IDENTITY,
    SERVER_SHUTDOWN,
    PARTICIPANT_REMOVED,
    ROOM_DELETED,
    STATE_MISMATCH,
    JOIN_FAILURE,
    MIGRATION,
    SIGNAL_CLOSE,
    ROOM_CLOSED,
    USER_UNAVAILABLE,
    USER_REJECTED,
    SIP_TRUNK_FAILURE
}
