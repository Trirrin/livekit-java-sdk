package io.livekit.sdk.rtc;

import dev.onvoid.webrtc.RTCIceCandidate;
import dev.onvoid.webrtc.RTCIceConnectionState;
import dev.onvoid.webrtc.media.MediaStreamTrack;

/**
 * Listener for RTC engine events.
 */
public interface RtcEngineListener {

    /**
     * Called when a local ICE candidate is generated.
     */
    void onIceCandidate(RTCIceCandidate candidate, int target);

    /**
     * Called when publisher ICE connection state changes.
     */
    void onPublisherIceConnectionChange(RTCIceConnectionState state);

    /**
     * Called when subscriber ICE connection state changes.
     */
    void onSubscriberIceConnectionChange(RTCIceConnectionState state);

    /**
     * Called when a remote track is received from subscriber connection.
     */
    void onRemoteTrackReceived(MediaStreamTrack track, String[] streamIds);

    /**
     * Called when a remote track is removed.
     */
    void onRemoteTrackRemoved(MediaStreamTrack track);

    /**
     * Called when a data channel message is received.
     */
    void onDataReceived(byte[] data, boolean reliable);

    /**
     * Called when publisher needs renegotiation.
     */
    void onPublisherNegotiationNeeded();

    /**
     * Called when an error occurs.
     */
    void onError(String error);
}
