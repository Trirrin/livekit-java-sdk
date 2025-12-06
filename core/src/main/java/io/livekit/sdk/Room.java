package io.livekit.sdk;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Represents a LiveKit room.
 * This is the main entry point for interacting with a LiveKit room.
 */
public class Room {
    private String sid;
    private String name;
    private String metadata;
    private ConnectionState state;
    private LocalParticipant localParticipant;
    private final Map<String, RemoteParticipant> remoteParticipants;
    private final List<RoomListener> listeners;
    private final RoomOptions options;

    public Room() {
        this(new RoomOptions());
    }

    public Room(RoomOptions options) {
        this.options = options;
        this.state = ConnectionState.DISCONNECTED;
        this.remoteParticipants = new ConcurrentHashMap<>();
        this.listeners = new CopyOnWriteArrayList<>();
    }

    /**
     * Connect to a LiveKit room.
     *
     * @param url   WebSocket URL of the LiveKit server
     * @param token Access token for authentication
     */
    public void connect(String url, String token) {
        if (state != ConnectionState.DISCONNECTED) {
            throw new IllegalStateException("Already connected or connecting");
        }
        setState(ConnectionState.CONNECTING);
        // Will be implemented with signaling module
    }

    /**
     * Disconnect from the room.
     */
    public void disconnect() {
        if (state == ConnectionState.DISCONNECTED) {
            return;
        }
        // Send leave request via signaling
        setState(ConnectionState.DISCONNECTED);
        notifyDisconnected(DisconnectReason.CLIENT_INITIATED);
    }

    /**
     * Send data to other participants.
     */
    public void publishData(DataPacket packet) {
        if (state != ConnectionState.CONNECTED) {
            throw new IllegalStateException("Not connected");
        }
        // Will be implemented with RTC data channel
    }

    public String getSid() {
        return sid;
    }

    void setSid(String sid) {
        this.sid = sid;
    }

    public String getName() {
        return name;
    }

    void setName(String name) {
        this.name = name;
    }

    public String getMetadata() {
        return metadata;
    }

    void setMetadata(String metadata) {
        String prev = this.metadata;
        this.metadata = metadata;
        if (prev != null && !prev.equals(metadata)) {
            notifyRoomMetadataChanged(metadata);
        }
    }

    public ConnectionState getState() {
        return state;
    }

    void setState(ConnectionState state) {
        this.state = state;
    }

    public LocalParticipant getLocalParticipant() {
        return localParticipant;
    }

    void setLocalParticipant(LocalParticipant localParticipant) {
        this.localParticipant = localParticipant;
    }

    public Map<String, RemoteParticipant> getRemoteParticipants() {
        return Collections.unmodifiableMap(remoteParticipants);
    }

    public RemoteParticipant getRemoteParticipant(String identity) {
        return remoteParticipants.get(identity);
    }

    void addRemoteParticipant(RemoteParticipant participant) {
        remoteParticipants.put(participant.getIdentity(), participant);
        notifyParticipantConnected(participant);
    }

    void removeRemoteParticipant(String identity) {
        RemoteParticipant participant = remoteParticipants.remove(identity);
        if (participant != null) {
            notifyParticipantDisconnected(participant);
        }
    }

    public RoomOptions getOptions() {
        return options;
    }

    // Listener management
    public void addListener(RoomListener listener) {
        listeners.add(listener);
    }

    public void removeListener(RoomListener listener) {
        listeners.remove(listener);
    }

    // Event notification methods
    void notifyConnected() {
        for (RoomListener listener : listeners) {
            listener.onConnected(this);
        }
    }

    void notifyDisconnected(DisconnectReason reason) {
        for (RoomListener listener : listeners) {
            listener.onDisconnected(this, reason);
        }
    }

    void notifyReconnecting() {
        for (RoomListener listener : listeners) {
            listener.onReconnecting(this);
        }
    }

    void notifyReconnected() {
        for (RoomListener listener : listeners) {
            listener.onReconnected(this);
        }
    }

    void notifyParticipantConnected(RemoteParticipant participant) {
        for (RoomListener listener : listeners) {
            listener.onParticipantConnected(this, participant);
        }
    }

    void notifyParticipantDisconnected(RemoteParticipant participant) {
        for (RoomListener listener : listeners) {
            listener.onParticipantDisconnected(this, participant);
        }
    }

    void notifyTrackPublished(TrackPublication publication, Participant participant) {
        for (RoomListener listener : listeners) {
            listener.onTrackPublished(this, publication, participant);
        }
    }

    void notifyTrackUnpublished(TrackPublication publication, Participant participant) {
        for (RoomListener listener : listeners) {
            listener.onTrackUnpublished(this, publication, participant);
        }
    }

    void notifyTrackSubscribed(Track track, TrackPublication publication, 
                               RemoteParticipant participant) {
        for (RoomListener listener : listeners) {
            listener.onTrackSubscribed(this, track, publication, participant);
        }
    }

    void notifyTrackUnsubscribed(Track track, TrackPublication publication,
                                  RemoteParticipant participant) {
        for (RoomListener listener : listeners) {
            listener.onTrackUnsubscribed(this, track, publication, participant);
        }
    }

    void notifyTrackMuted(TrackPublication publication, Participant participant) {
        for (RoomListener listener : listeners) {
            listener.onTrackMuted(this, publication, participant);
        }
    }

    void notifyTrackUnmuted(TrackPublication publication, Participant participant) {
        for (RoomListener listener : listeners) {
            listener.onTrackUnmuted(this, publication, participant);
        }
    }

    void notifyDataReceived(byte[] data, RemoteParticipant participant,
                            DataPacket.Kind kind, String topic) {
        for (RoomListener listener : listeners) {
            listener.onDataReceived(this, data, participant, kind, topic);
        }
    }

    void notifyActiveSpeakersChanged(List<Participant> speakers) {
        for (RoomListener listener : listeners) {
            listener.onActiveSpeakersChanged(this, speakers);
        }
    }

    void notifyConnectionQualityChanged(Participant participant, ConnectionQuality quality) {
        for (RoomListener listener : listeners) {
            listener.onConnectionQualityChanged(this, participant, quality);
        }
    }

    void notifyRoomMetadataChanged(String metadata) {
        for (RoomListener listener : listeners) {
            listener.onRoomMetadataChanged(this, metadata);
        }
    }
}
