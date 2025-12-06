package io.livekit.sdk;

/**
 * Represents a remote participant in the room.
 */
public class RemoteParticipant extends Participant {

    public RemoteParticipant(String sid, String identity) {
        super(sid, identity);
    }

    /**
     * Subscribe to a remote track publication.
     */
    public void subscribe(TrackPublication publication) {
        // Will be implemented with RTC transport
    }

    /**
     * Unsubscribe from a remote track publication.
     */
    public void unsubscribe(TrackPublication publication) {
        // Will be implemented with RTC transport
    }

    /**
     * Update track subscription permissions.
     */
    public void setSubscriptionPermissions(boolean allowed) {
        // Will be implemented with signaling
    }
}
