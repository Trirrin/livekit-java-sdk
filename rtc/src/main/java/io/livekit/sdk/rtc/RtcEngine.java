package io.livekit.sdk.rtc;

import dev.onvoid.webrtc.RTCIceCandidate;
import dev.onvoid.webrtc.RTCSessionDescription;
import java.util.List;

/**
 * Interface for WebRTC operations. Abstracts PeerConnection management for publish and subscribe
 * connections.
 */
public interface RtcEngine {

  /** Initialize the RTC engine with ICE servers. */
  void initialize(List<IceServerConfig> iceServers);

  /** Create an offer for the publisher connection. */
  void createPublisherOffer(SdpCallback callback);

  /** Set the remote answer for the publisher connection. */
  void setPublisherAnswer(RTCSessionDescription answer, SdpCallback callback);

  /** Handle an offer from the server for the subscriber connection. */
  void handleSubscriberOffer(RTCSessionDescription offer, SdpCallback callback);

  /** Add an ICE candidate to the appropriate connection. */
  void addIceCandidate(RTCIceCandidate candidate, int target);

  /** Add a local audio track to publish. */
  void addAudioTrack(LocalAudioTrack track);

  /** Add a local video track to publish. */
  void addVideoTrack(LocalVideoTrack track);

  /** Remove a local track. */
  void removeTrack(String trackId);

  /** Set track muted state. */
  void setTrackMuted(String trackId, boolean muted);

  /** Close all connections and release resources. */
  void close();

  /** Set the RTC engine listener. */
  void setListener(RtcEngineListener listener);

  /** ICE connection target constants. */
  int TARGET_PUBLISHER = 0;

  int TARGET_SUBSCRIBER = 1;

  /** Callback for SDP operations. */
  interface SdpCallback {
    void onSuccess(RTCSessionDescription description);

    void onFailure(String error);
  }
}
