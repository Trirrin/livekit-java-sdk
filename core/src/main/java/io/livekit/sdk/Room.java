package io.livekit.sdk;

import livekit.LivekitModels;
import livekit.LivekitRtc;

import java.util.ArrayList;
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
    private RoomSignalHandler signalHandler;

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
     * Get the signal handler for this room.
     * Used by signaling module to integrate with the room.
     */
    public RoomSignalHandler getSignalHandler() {
        if (signalHandler == null) {
            signalHandler = new RoomSignalHandler(this);
        }
        return signalHandler;
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
        // Actual connection is handled by LiveKitClient which coordinates Room + SignalClient
    }

    /**
     * Disconnect from the room.
     */
    public void disconnect() {
        if (state == ConnectionState.DISCONNECTED) {
            return;
        }
        setState(ConnectionState.DISCONNECTED);
        clearParticipants();
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

    void clearParticipants() {
        remoteParticipants.clear();
        localParticipant = null;
    }

    // Signal handler callbacks for processing server messages
    void handleJoinResponse(LivekitRtc.JoinResponse response) {
        this.sid = response.getRoom().getSid();
        this.name = response.getRoom().getName();
        this.metadata = response.getRoom().getMetadata();

        // Create local participant
        this.localParticipant = ProtoConverter.localParticipantFromProto(response.getParticipant());

        // Add local participant tracks
        for (LivekitModels.TrackInfo trackInfo : response.getParticipant().getTracksList()) {
            TrackPublication pub = ProtoConverter.trackPublicationFromProto(trackInfo);
            localParticipant.addTrackPublication(pub);
        }

        // Process other participants
        for (LivekitModels.ParticipantInfo info : response.getOtherParticipantsList()) {
            handleParticipantInfo(info);
        }

        setState(ConnectionState.CONNECTED);
        notifyConnected();
    }

    void handleParticipantUpdate(LivekitRtc.ParticipantUpdate update) {
        for (LivekitModels.ParticipantInfo info : update.getParticipantsList()) {
            handleParticipantInfo(info);
        }
    }

    private void handleParticipantInfo(LivekitModels.ParticipantInfo info) {
        // Skip if this is us
        if (localParticipant != null && info.getSid().equals(localParticipant.getSid())) {
            ProtoConverter.updateParticipantFromProto(localParticipant, info);
            syncParticipantTracks(localParticipant, info.getTracksList());
            return;
        }

        RemoteParticipant participant = remoteParticipants.get(info.getIdentity());

        if (info.getState() == LivekitModels.ParticipantInfo.State.DISCONNECTED) {
            // Participant left
            if (participant != null) {
                removeRemoteParticipant(info.getIdentity());
            }
            return;
        }

        boolean isNew = (participant == null);
        if (isNew) {
            participant = ProtoConverter.remoteParticipantFromProto(info);
            remoteParticipants.put(info.getIdentity(), participant);
        } else {
            ProtoConverter.updateParticipantFromProto(participant, info);
        }

        syncParticipantTracks(participant, info.getTracksList());

        if (isNew) {
            notifyParticipantConnected(participant);
        }
    }

    private void syncParticipantTracks(Participant participant, List<LivekitModels.TrackInfo> trackInfos) {
        Map<String, TrackPublication> currentPubs = participant.getTrackPublications();

        // Track which sids we've seen
        java.util.Set<String> seenSids = new java.util.HashSet<>();

        for (LivekitModels.TrackInfo info : trackInfos) {
            seenSids.add(info.getSid());

            TrackPublication existing = currentPubs.get(info.getSid());
            if (existing != null) {
                boolean wasMuted = existing.isMuted();
                ProtoConverter.updateTrackPublicationFromProto(existing, info);
                if (wasMuted != existing.isMuted()) {
                    if (existing.isMuted()) {
                        notifyTrackMuted(existing, participant);
                    } else {
                        notifyTrackUnmuted(existing, participant);
                    }
                }
            } else {
                TrackPublication pub = ProtoConverter.trackPublicationFromProto(info);
                participant.addTrackPublication(pub);
                notifyTrackPublished(pub, participant);
            }
        }

        // Remove tracks no longer present
        List<String> toRemove = new ArrayList<>();
        for (String sid : currentPubs.keySet()) {
            if (!seenSids.contains(sid)) {
                toRemove.add(sid);
            }
        }
        for (String sid : toRemove) {
            TrackPublication pub = currentPubs.get(sid);
            participant.removeTrackPublication(sid);
            notifyTrackUnpublished(pub, participant);
        }
    }

    void handleRoomUpdate(LivekitRtc.RoomUpdate update) {
        LivekitModels.Room room = update.getRoom();
        this.sid = room.getSid();
        this.name = room.getName();
        String prevMetadata = this.metadata;
        this.metadata = room.getMetadata();
        if (prevMetadata != null && !prevMetadata.equals(this.metadata)) {
            notifyRoomMetadataChanged(this.metadata);
        }
    }

    void handleSpeakersChanged(LivekitRtc.SpeakersChanged changed) {
        List<Participant> activeSpeakers = new ArrayList<>();
        for (LivekitModels.SpeakerInfo speaker : changed.getSpeakersList()) {
            Participant p = findParticipantBySid(speaker.getSid());
            if (p != null) {
                p.setSpeaking(speaker.getActive());
                p.setAudioLevel((long) (speaker.getLevel() * 100));
                if (speaker.getActive()) {
                    activeSpeakers.add(p);
                }
            }
        }
        notifyActiveSpeakersChanged(activeSpeakers);
    }

    void handleConnectionQualityUpdate(LivekitRtc.ConnectionQualityUpdate update) {
        for (LivekitRtc.ConnectionQualityInfo info : update.getUpdatesList()) {
            Participant p = findParticipantBySid(info.getParticipantSid());
            if (p != null) {
                ConnectionQuality quality = ProtoConverter.fromProto(info.getQuality());
                p.setConnectionQuality(quality);
                notifyConnectionQualityChanged(p, quality);
            }
        }
    }

    void handleMuteTrack(LivekitRtc.MuteTrackRequest mute) {
        if (localParticipant == null) return;
        TrackPublication pub = localParticipant.getTrackPublication(mute.getSid());
        if (pub != null) {
            pub.setMuted(mute.getMuted());
            if (mute.getMuted()) {
                notifyTrackMuted(pub, localParticipant);
            } else {
                notifyTrackUnmuted(pub, localParticipant);
            }
        }
    }

    void handleLeave(LivekitRtc.LeaveRequest leave) {
        DisconnectReason reason = ProtoConverter.fromProto(leave.getReason());
        setState(ConnectionState.DISCONNECTED);
        clearParticipants();
        notifyDisconnected(reason);
    }

    void handleReconnecting() {
        setState(ConnectionState.RECONNECTING);
        notifyReconnecting();
    }

    void handleReconnected() {
        setState(ConnectionState.CONNECTED);
        notifyReconnected();
    }

    void handleSignalError(Exception e) {
        // Could implement error callback
    }

    private Participant findParticipantBySid(String sid) {
        if (localParticipant != null && localParticipant.getSid().equals(sid)) {
            return localParticipant;
        }
        for (RemoteParticipant p : remoteParticipants.values()) {
            if (p.getSid().equals(sid)) {
                return p;
            }
        }
        return null;
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
