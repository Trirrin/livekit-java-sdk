package io.livekit.sdk;

/**
 * Listener interface for room events.
 */
public interface RoomListener {
    
    default void onConnected(Room room) {}
    
    default void onDisconnected(Room room, DisconnectReason reason) {}
    
    default void onReconnecting(Room room) {}
    
    default void onReconnected(Room room) {}
    
    default void onParticipantConnected(Room room, RemoteParticipant participant) {}
    
    default void onParticipantDisconnected(Room room, RemoteParticipant participant) {}
    
    default void onParticipantMetadataChanged(Room room, Participant participant, String prevMetadata) {}
    
    default void onTrackPublished(Room room, TrackPublication publication, Participant participant) {}
    
    default void onTrackUnpublished(Room room, TrackPublication publication, Participant participant) {}
    
    default void onTrackSubscribed(Room room, Track track, TrackPublication publication, 
                                   RemoteParticipant participant) {}
    
    default void onTrackUnsubscribed(Room room, Track track, TrackPublication publication,
                                     RemoteParticipant participant) {}
    
    default void onTrackMuted(Room room, TrackPublication publication, Participant participant) {}
    
    default void onTrackUnmuted(Room room, TrackPublication publication, Participant participant) {}
    
    default void onDataReceived(Room room, byte[] data, RemoteParticipant participant,
                               DataPacket.Kind kind, String topic) {}
    
    default void onActiveSpeakersChanged(Room room, java.util.List<Participant> speakers) {}
    
    default void onConnectionQualityChanged(Room room, Participant participant, ConnectionQuality quality) {}
    
    default void onRoomMetadataChanged(Room room, String metadata) {}
}
