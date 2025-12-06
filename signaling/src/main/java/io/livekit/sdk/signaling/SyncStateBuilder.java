package io.livekit.sdk.signaling;

import livekit.LivekitRtc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Builder for SyncState message used during reconnection.
 * Captures client state to sync with server after resume.
 */
public class SyncStateBuilder {
    private LivekitRtc.SessionDescription answer;
    private LivekitRtc.SessionDescription offer;
    private final List<String> subscribedTrackSids = new ArrayList<>();
    private final List<LivekitRtc.TrackPublishedResponse> publishedTracks = new ArrayList<>();
    private final List<LivekitRtc.DataChannelInfo> dataChannels = new ArrayList<>();
    private final List<String> disabledTrackSids = new ArrayList<>();
    private final Map<String, Integer> dataChannelReceiveStates = new HashMap<>();

    public SyncStateBuilder setAnswer(LivekitRtc.SessionDescription answer) {
        this.answer = answer;
        return this;
    }

    public SyncStateBuilder setOffer(LivekitRtc.SessionDescription offer) {
        this.offer = offer;
        return this;
    }

    public SyncStateBuilder addSubscribedTrack(String trackSid) {
        subscribedTrackSids.add(trackSid);
        return this;
    }

    public SyncStateBuilder setSubscribedTracks(List<String> trackSids) {
        subscribedTrackSids.clear();
        subscribedTrackSids.addAll(trackSids);
        return this;
    }

    public SyncStateBuilder addPublishedTrack(LivekitRtc.TrackPublishedResponse track) {
        publishedTracks.add(track);
        return this;
    }

    public SyncStateBuilder addPublishedTrack(String cid, String sid, String name,
                                               livekit.LivekitModels.TrackType type) {
        LivekitRtc.TrackPublishedResponse response = LivekitRtc.TrackPublishedResponse.newBuilder()
                .setCid(cid)
                .setTrack(livekit.LivekitModels.TrackInfo.newBuilder()
                        .setSid(sid)
                        .setName(name)
                        .setType(type)
                        .build())
                .build();
        publishedTracks.add(response);
        return this;
    }

    public SyncStateBuilder addDataChannel(String label, int id, LivekitRtc.SignalTarget target) {
        LivekitRtc.DataChannelInfo info = LivekitRtc.DataChannelInfo.newBuilder()
                .setLabel(label)
                .setId(id)
                .setTarget(target)
                .build();
        dataChannels.add(info);
        return this;
    }

    public SyncStateBuilder addDisabledTrack(String trackSid) {
        disabledTrackSids.add(trackSid);
        return this;
    }

    public SyncStateBuilder setDataChannelReceiveState(String publisherSid, int lastSeq) {
        dataChannelReceiveStates.put(publisherSid, lastSeq);
        return this;
    }

    public LivekitRtc.SyncState build() {
        LivekitRtc.SyncState.Builder builder = LivekitRtc.SyncState.newBuilder();

        if (answer != null) {
            builder.setAnswer(answer);
        }

        if (offer != null) {
            builder.setOffer(offer);
        }

        if (!subscribedTrackSids.isEmpty()) {
            builder.setSubscription(LivekitRtc.UpdateSubscription.newBuilder()
                    .addAllTrackSids(subscribedTrackSids)
                    .setSubscribe(true)
                    .build());
        }

        builder.addAllPublishTracks(publishedTracks);
        builder.addAllDataChannels(dataChannels);
        builder.addAllTrackSidsDisabled(disabledTrackSids);

        for (Map.Entry<String, Integer> entry : dataChannelReceiveStates.entrySet()) {
            builder.addDatachannelReceiveStates(LivekitRtc.DataChannelReceiveState.newBuilder()
                    .setPublisherSid(entry.getKey())
                    .setLastSeq(entry.getValue())
                    .build());
        }

        return builder.build();
    }

    public void clear() {
        answer = null;
        offer = null;
        subscribedTrackSids.clear();
        publishedTracks.clear();
        dataChannels.clear();
        disabledTrackSids.clear();
        dataChannelReceiveStates.clear();
    }
}
