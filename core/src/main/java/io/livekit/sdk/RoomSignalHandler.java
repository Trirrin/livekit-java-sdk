package io.livekit.sdk;

import livekit.LivekitRtc;

import java.util.List;

/**
 * Handles signaling events and delegates them to Room for state management.
 * This class implements the interface expected by SignalClient to bridge
 * signaling layer with Room state management.
 */
public class RoomSignalHandler {
    private final Room room;

    public RoomSignalHandler(Room room) {
        this.room = room;
    }

    public void onStateChanged(Object state) {
        // SignalState is in signaling module, use string comparison or enum value
        String stateName = state.toString();
        if ("RECONNECTING".equals(stateName)) {
            room.handleReconnecting();
        } else if ("CONNECTED".equals(stateName)) {
            // Connected is handled via JoinResponse
        } else if ("DISCONNECTED".equals(stateName) || "FAILED".equals(stateName)) {
            // Disconnect is handled via Leave or explicit disconnect
        }
    }

    public void onJoinResponse(LivekitRtc.JoinResponse response) {
        room.handleJoinResponse(response);
    }

    public void onAnswer(LivekitRtc.SessionDescription answer) {
        // RTC layer responsibility
    }

    public void onOffer(LivekitRtc.SessionDescription offer) {
        // RTC layer responsibility
    }

    public void onTrickle(LivekitRtc.TrickleRequest trickle) {
        // RTC layer responsibility
    }

    public void onParticipantUpdate(LivekitRtc.ParticipantUpdate update) {
        room.handleParticipantUpdate(update);
    }

    public void onTrackPublished(LivekitRtc.TrackPublishedResponse response) {
        // Local track published confirmation - RTC layer handles this
    }

    public void onTrackUnpublished(LivekitRtc.TrackUnpublishedResponse response) {
        // Local track unpublished confirmation
    }

    public void onLeave(LivekitRtc.LeaveRequest leave) {
        room.handleLeave(leave);
    }

    public void onMuteTrack(LivekitRtc.MuteTrackRequest mute) {
        room.handleMuteTrack(mute);
    }

    public void onSpeakersChanged(LivekitRtc.SpeakersChanged speakersChanged) {
        room.handleSpeakersChanged(speakersChanged);
    }

    public void onRoomUpdate(LivekitRtc.RoomUpdate roomUpdate) {
        room.handleRoomUpdate(roomUpdate);
    }

    public void onConnectionQuality(LivekitRtc.ConnectionQualityUpdate quality) {
        room.handleConnectionQualityUpdate(quality);
    }

    public void onStreamStateUpdate(LivekitRtc.StreamStateUpdate streamState) {
        // Stream state handling for adaptive streaming
    }

    public void onRefreshToken(String token) {
        // Token refresh handled by SignalClient
    }

    public void onReconnectResponse(LivekitRtc.ReconnectResponse response) {
        room.handleReconnected();
    }

    public void onPong(long timestamp) {
        // Keepalive handled by SignalClient
    }

    public void onError(Exception e) {
        room.handleSignalError(e);
    }

    public void onIceServersUpdated(List<LivekitRtc.ICEServer> iceServers) {
        // ICE servers for RTC layer
    }

    public void onIceRestartRequired(Object reason) {
        // ICE restart for RTC layer
    }

    public Room getRoom() {
        return room;
    }
}
