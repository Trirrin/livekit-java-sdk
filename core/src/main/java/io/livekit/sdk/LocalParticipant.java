package io.livekit.sdk;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents the local participant (current user) in the room.
 */
public class LocalParticipant extends Participant {
    private final Map<String, TrackPublication> localTrackPublications;

    public LocalParticipant(String sid, String identity) {
        super(sid, identity);
        this.localTrackPublications = new ConcurrentHashMap<>();
    }

    /**
     * Publish a local track to the room.
     * This is a placeholder - actual implementation requires RTC transport.
     */
    public void publishTrack(Track track) {
        // Will be implemented with RTC transport
    }

    /**
     * Unpublish a local track.
     */
    public void unpublishTrack(Track track) {
        // Will be implemented with RTC transport
    }

    /**
     * Set microphone enabled state.
     */
    public void setMicrophoneEnabled(boolean enabled) {
        // Will be implemented with RTC transport
    }

    /**
     * Set camera enabled state.
     */
    public void setCameraEnabled(boolean enabled) {
        // Will be implemented with RTC transport
    }

    /**
     * Set screen share enabled state.
     */
    public void setScreenShareEnabled(boolean enabled) {
        // Will be implemented with RTC transport
    }

    /**
     * Update participant metadata.
     */
    public void setParticipantMetadata(String metadata) {
        this.metadata = metadata;
        // Send update to server via signaling
    }

    /**
     * Update participant name.
     */
    public void setParticipantName(String name) {
        this.name = name;
        // Send update to server via signaling
    }

    /**
     * Update participant attributes.
     */
    public void setParticipantAttributes(Map<String, String> attributes) {
        setAttributes(attributes);
        // Send update to server via signaling
    }
}
