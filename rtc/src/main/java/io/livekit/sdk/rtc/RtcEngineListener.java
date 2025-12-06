package io.livekit.sdk.rtc;

import dev.onvoid.webrtc.RTCIceCandidate;
import dev.onvoid.webrtc.RTCIceConnectionState;
import dev.onvoid.webrtc.RTCRtpTransceiver;
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
     * @param track The received media track
     * @param transceiver The transceiver containing the track (used to get mid)
     * @param streamIds Stream IDs associated with the track
     */
    void onRemoteTrackReceived(MediaStreamTrack track, RTCRtpTransceiver transceiver, String[] streamIds);

    /**
     * Called when a remote track is removed.
     */
    void onRemoteTrackRemoved(MediaStreamTrack track);

    /**
     * Called when a data channel message is received.
     * @param data The received data
     * @param reliable Whether the data was sent reliably
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
