package io.livekit.sdk.rtc;

import dev.onvoid.webrtc.*;
import dev.onvoid.webrtc.media.MediaStreamTrack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebRTC engine implementation using webrtc-java library.
 * Manages publisher and subscriber peer connections.
 */
public class PeerConnectionEngine implements RtcEngine {

    private PeerConnectionFactory factory;
    private RTCPeerConnection publisherPc;
    private RTCPeerConnection subscriberPc;
    private RTCConfiguration rtcConfig;
    private RtcEngineListener listener;

    private final Map<String, RTCRtpSender> trackSenders = new ConcurrentHashMap<>();
    private final List<String> streamIds = new ArrayList<>();

    private volatile boolean initialized = false;

    public PeerConnectionEngine() {
        streamIds.add("livekit");
    }

    @Override
    public void initialize(List<IceServerConfig> iceServers) {
        if (initialized) {
            return;
        }

        factory = new PeerConnectionFactory();

        rtcConfig = new RTCConfiguration();
        rtcConfig.bundlePolicy = RTCBundlePolicy.MAX_BUNDLE;

        for (IceServerConfig config : iceServers) {
            RTCIceServer iceServer = new RTCIceServer();
            for (String url : config.getUrls()) {
                iceServer.urls.add(url);
            }
            if (config.getUsername() != null) {
                iceServer.username = config.getUsername();
            }
            if (config.getCredential() != null) {
                iceServer.password = config.getCredential();
            }
            rtcConfig.iceServers.add(iceServer);
        }

        publisherPc = factory.createPeerConnection(rtcConfig, new PublisherObserver());
        subscriberPc = factory.createPeerConnection(rtcConfig, new SubscriberObserver());

        initialized = true;
    }

    @Override
    public void createPublisherOffer(SdpCallback callback) {
        if (publisherPc == null) {
            callback.onFailure("Publisher connection not initialized");
            return;
        }

        RTCOfferOptions options = new RTCOfferOptions();
        publisherPc.createOffer(options, new CreateSessionDescriptionObserver() {
            @Override
            public void onSuccess(RTCSessionDescription description) {
                publisherPc.setLocalDescription(description, new SetSessionDescriptionObserver() {
                    @Override
                    public void onSuccess() {
                        callback.onSuccess(description);
                    }

                    @Override
                    public void onFailure(String error) {
                        callback.onFailure("Failed to set local description: " + error);
                    }
                });
            }

            @Override
            public void onFailure(String error) {
                callback.onFailure("Failed to create offer: " + error);
            }
        });
    }

    @Override
    public void setPublisherAnswer(RTCSessionDescription answer, SdpCallback callback) {
        if (publisherPc == null) {
            callback.onFailure("Publisher connection not initialized");
            return;
        }

        publisherPc.setRemoteDescription(answer, new SetSessionDescriptionObserver() {
            @Override
            public void onSuccess() {
                callback.onSuccess(answer);
            }

            @Override
            public void onFailure(String error) {
                callback.onFailure("Failed to set remote description: " + error);
            }
        });
    }

    @Override
    public void handleSubscriberOffer(RTCSessionDescription offer, SdpCallback callback) {
        if (subscriberPc == null) {
            callback.onFailure("Subscriber connection not initialized");
            return;
        }

        subscriberPc.setRemoteDescription(offer, new SetSessionDescriptionObserver() {
            @Override
            public void onSuccess() {
                RTCAnswerOptions options = new RTCAnswerOptions();
                subscriberPc.createAnswer(options, new CreateSessionDescriptionObserver() {
                    @Override
                    public void onSuccess(RTCSessionDescription answer) {
                        subscriberPc.setLocalDescription(answer, new SetSessionDescriptionObserver() {
                            @Override
                            public void onSuccess() {
                                callback.onSuccess(answer);
                            }

                            @Override
                            public void onFailure(String error) {
                                callback.onFailure("Failed to set local description: " + error);
                            }
                        });
                    }

                    @Override
                    public void onFailure(String error) {
                        callback.onFailure("Failed to create answer: " + error);
                    }
                });
            }

            @Override
            public void onFailure(String error) {
                callback.onFailure("Failed to set remote offer: " + error);
            }
        });
    }

    @Override
    public void addIceCandidate(RTCIceCandidate candidate, int target) {
        RTCPeerConnection pc = (target == TARGET_PUBLISHER) ? publisherPc : subscriberPc;
        if (pc != null) {
            pc.addIceCandidate(candidate);
        }
    }

    @Override
    public void addAudioTrack(LocalAudioTrack track) {
        if (publisherPc == null || track.getNativeTrack() == null) {
            return;
        }

        RTCRtpSender sender = publisherPc.addTrack(track.getNativeTrack(), streamIds);
        if (sender != null) {
            trackSenders.put(track.getId(), sender);
        }
    }

    @Override
    public void addVideoTrack(LocalVideoTrack track) {
        if (publisherPc == null || track.getNativeTrack() == null) {
            return;
        }

        RTCRtpSender sender = publisherPc.addTrack(track.getNativeTrack(), streamIds);
        if (sender != null) {
            trackSenders.put(track.getId(), sender);
        }
    }

    @Override
    public void removeTrack(String trackId) {
        RTCRtpSender sender = trackSenders.remove(trackId);
        if (sender != null && publisherPc != null) {
            publisherPc.removeTrack(sender);
        }
    }

    @Override
    public void setTrackMuted(String trackId, boolean muted) {
        RTCRtpSender sender = trackSenders.get(trackId);
        if (sender != null && sender.getTrack() != null) {
            sender.getTrack().setEnabled(!muted);
        }
    }

    @Override
    public void close() {
        if (publisherPc != null) {
            publisherPc.close();
            publisherPc = null;
        }
        if (subscriberPc != null) {
            subscriberPc.close();
            subscriberPc = null;
        }
        if (factory != null) {
            factory.dispose();
            factory = null;
        }
        trackSenders.clear();
        initialized = false;
    }

    @Override
    public void setListener(RtcEngineListener listener) {
        this.listener = listener;
    }

    /**
     * Update ICE servers configuration for ICE restart.
     */
    public void updateIceServers(List<IceServerConfig> iceServers) {
        rtcConfig.iceServers.clear();
        for (IceServerConfig config : iceServers) {
            RTCIceServer iceServer = new RTCIceServer();
            for (String url : config.getUrls()) {
                iceServer.urls.add(url);
            }
            if (config.getUsername() != null) {
                iceServer.username = config.getUsername();
            }
            if (config.getCredential() != null) {
                iceServer.password = config.getCredential();
            }
            rtcConfig.iceServers.add(iceServer);
        }

        if (publisherPc != null) {
            publisherPc.setConfiguration(rtcConfig);
        }
        if (subscriberPc != null) {
            subscriberPc.setConfiguration(rtcConfig);
        }
    }

    /**
     * Trigger ICE restart on publisher connection.
     */
    public void restartPublisherIce() {
        if (publisherPc != null) {
            publisherPc.restartIce();
        }
    }

    /**
     * Trigger ICE restart on subscriber connection.
     */
    public void restartSubscriberIce() {
        if (subscriberPc != null) {
            subscriberPc.restartIce();
        }
    }

    // Publisher PeerConnection observer
    private class PublisherObserver implements PeerConnectionObserver {
        @Override
        public void onIceCandidate(RTCIceCandidate candidate) {
            if (listener != null) {
                listener.onIceCandidate(candidate, TARGET_PUBLISHER);
            }
        }

        @Override
        public void onIceConnectionChange(RTCIceConnectionState state) {
            if (listener != null) {
                listener.onPublisherIceConnectionChange(state);
            }
        }

        @Override
        public void onSignalingChange(RTCSignalingState state) {}

        @Override
        public void onIceGatheringChange(RTCIceGatheringState state) {}

        @Override
        public void onRenegotiationNeeded() {
            if (listener != null) {
                listener.onPublisherNegotiationNeeded();
            }
        }

        @Override
        public void onConnectionChange(RTCPeerConnectionState state) {}

        @Override
        public void onAddStream(dev.onvoid.webrtc.media.MediaStream stream) {}

        @Override
        public void onRemoveStream(dev.onvoid.webrtc.media.MediaStream stream) {}

        @Override
        public void onDataChannel(RTCDataChannel dataChannel) {}

        @Override
        public void onTrack(RTCRtpTransceiver transceiver) {}
    }

    // Subscriber PeerConnection observer
    private class SubscriberObserver implements PeerConnectionObserver {
        @Override
        public void onIceCandidate(RTCIceCandidate candidate) {
            if (listener != null) {
                listener.onIceCandidate(candidate, TARGET_SUBSCRIBER);
            }
        }

        @Override
        public void onIceConnectionChange(RTCIceConnectionState state) {
            if (listener != null) {
                listener.onSubscriberIceConnectionChange(state);
            }
        }

        @Override
        public void onSignalingChange(RTCSignalingState state) {}

        @Override
        public void onIceGatheringChange(RTCIceGatheringState state) {}

        @Override
        public void onRenegotiationNeeded() {}

        @Override
        public void onConnectionChange(RTCPeerConnectionState state) {}

        @Override
        public void onAddStream(dev.onvoid.webrtc.media.MediaStream stream) {}

        @Override
        public void onRemoveStream(dev.onvoid.webrtc.media.MediaStream stream) {}

        @Override
        public void onDataChannel(RTCDataChannel dataChannel) {}

        @Override
        public void onTrack(RTCRtpTransceiver transceiver) {
            if (listener != null && transceiver.getReceiver() != null) {
                MediaStreamTrack track = transceiver.getReceiver().getTrack();
                if (track != null) {
                    listener.onRemoteTrackReceived(track, new String[]{"livekit"});
                }
            }
        }
    }
}
