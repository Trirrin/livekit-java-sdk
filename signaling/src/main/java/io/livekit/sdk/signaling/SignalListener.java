package io.livekit.sdk.signaling;

import livekit.LivekitRtc;

/**
 * Listener interface for signaling events.
 */
public interface SignalListener {
    
    void onStateChanged(SignalState state);
    
    void onJoinResponse(LivekitRtc.JoinResponse response);
    
    void onAnswer(LivekitRtc.SessionDescription answer);
    
    void onOffer(LivekitRtc.SessionDescription offer);
    
    void onTrickle(LivekitRtc.TrickleRequest trickle);
    
    void onParticipantUpdate(LivekitRtc.ParticipantUpdate update);
    
    void onTrackPublished(LivekitRtc.TrackPublishedResponse response);
    
    void onTrackUnpublished(LivekitRtc.TrackUnpublishedResponse response);
    
    void onLeave(LivekitRtc.LeaveRequest leave);
    
    void onMuteTrack(LivekitRtc.MuteTrackRequest mute);
    
    void onSpeakersChanged(LivekitRtc.SpeakersChanged speakersChanged);
    
    void onRoomUpdate(LivekitRtc.RoomUpdate roomUpdate);
    
    void onConnectionQuality(LivekitRtc.ConnectionQualityUpdate quality);
    
    void onStreamStateUpdate(LivekitRtc.StreamStateUpdate streamState);
    
    void onRefreshToken(String token);
    
    void onReconnectResponse(LivekitRtc.ReconnectResponse response);
    
    void onPong(long timestamp);
    
    void onError(Exception e);
}
