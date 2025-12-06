package io.livekit.sdk.signaling;

import livekit.LivekitModels;
import livekit.LivekitRtc;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SyncStateBuilderTest {

    @Test
    void testBuildEmptySyncState() {
        SyncStateBuilder builder = new SyncStateBuilder();
        LivekitRtc.SyncState syncState = builder.build();
        
        assertNotNull(syncState);
        assertFalse(syncState.hasAnswer());
        assertFalse(syncState.hasOffer());
        assertFalse(syncState.hasSubscription());
        assertEquals(0, syncState.getPublishTracksCount());
        assertEquals(0, syncState.getDataChannelsCount());
    }

    @Test
    void testBuildWithAnswer() {
        LivekitRtc.SessionDescription answer = LivekitRtc.SessionDescription.newBuilder()
                .setType("answer")
                .setSdp("v=0...")
                .build();
        
        SyncStateBuilder builder = new SyncStateBuilder();
        builder.setAnswer(answer);
        LivekitRtc.SyncState syncState = builder.build();
        
        assertTrue(syncState.hasAnswer());
        assertEquals("answer", syncState.getAnswer().getType());
        assertEquals("v=0...", syncState.getAnswer().getSdp());
    }

    @Test
    void testBuildWithOffer() {
        LivekitRtc.SessionDescription offer = LivekitRtc.SessionDescription.newBuilder()
                .setType("offer")
                .setSdp("offer-sdp")
                .build();
        
        SyncStateBuilder builder = new SyncStateBuilder();
        builder.setOffer(offer);
        LivekitRtc.SyncState syncState = builder.build();
        
        assertTrue(syncState.hasOffer());
        assertEquals("offer", syncState.getOffer().getType());
    }

    @Test
    void testBuildWithSubscribedTracks() {
        SyncStateBuilder builder = new SyncStateBuilder();
        builder.addSubscribedTrack("TR_123");
        builder.addSubscribedTrack("TR_456");
        LivekitRtc.SyncState syncState = builder.build();
        
        assertTrue(syncState.hasSubscription());
        assertEquals(2, syncState.getSubscription().getTrackSidsCount());
        assertEquals("TR_123", syncState.getSubscription().getTrackSids(0));
        assertEquals("TR_456", syncState.getSubscription().getTrackSids(1));
        assertTrue(syncState.getSubscription().getSubscribe());
    }

    @Test
    void testBuildWithPublishedTracks() {
        SyncStateBuilder builder = new SyncStateBuilder();
        builder.addPublishedTrack("cid1", "TR_PUB1", "camera", LivekitModels.TrackType.VIDEO);
        LivekitRtc.SyncState syncState = builder.build();
        
        assertEquals(1, syncState.getPublishTracksCount());
        assertEquals("cid1", syncState.getPublishTracks(0).getCid());
        assertEquals("TR_PUB1", syncState.getPublishTracks(0).getTrack().getSid());
        assertEquals("camera", syncState.getPublishTracks(0).getTrack().getName());
        assertEquals(LivekitModels.TrackType.VIDEO, syncState.getPublishTracks(0).getTrack().getType());
    }

    @Test
    void testBuildWithDataChannels() {
        SyncStateBuilder builder = new SyncStateBuilder();
        builder.addDataChannel("reliable", 1, LivekitRtc.SignalTarget.PUBLISHER);
        builder.addDataChannel("lossy", 2, LivekitRtc.SignalTarget.SUBSCRIBER);
        LivekitRtc.SyncState syncState = builder.build();
        
        assertEquals(2, syncState.getDataChannelsCount());
        assertEquals("reliable", syncState.getDataChannels(0).getLabel());
        assertEquals(1, syncState.getDataChannels(0).getId());
        assertEquals(LivekitRtc.SignalTarget.PUBLISHER, syncState.getDataChannels(0).getTarget());
    }

    @Test
    void testBuildWithDisabledTracks() {
        SyncStateBuilder builder = new SyncStateBuilder();
        builder.addDisabledTrack("TR_DISABLED1");
        builder.addDisabledTrack("TR_DISABLED2");
        LivekitRtc.SyncState syncState = builder.build();
        
        assertEquals(2, syncState.getTrackSidsDisabledCount());
        assertTrue(syncState.getTrackSidsDisabledList().contains("TR_DISABLED1"));
        assertTrue(syncState.getTrackSidsDisabledList().contains("TR_DISABLED2"));
    }

    @Test
    void testBuildWithDataChannelReceiveState() {
        SyncStateBuilder builder = new SyncStateBuilder();
        builder.setDataChannelReceiveState("PA_123", 42);
        builder.setDataChannelReceiveState("PA_456", 100);
        LivekitRtc.SyncState syncState = builder.build();
        
        assertEquals(2, syncState.getDatachannelReceiveStatesCount());
    }

    @Test
    void testClear() {
        SyncStateBuilder builder = new SyncStateBuilder();
        builder.setAnswer(LivekitRtc.SessionDescription.newBuilder().setType("answer").build());
        builder.addSubscribedTrack("TR_123");
        builder.addPublishedTrack("cid", "sid", "name", LivekitModels.TrackType.AUDIO);
        builder.addDataChannel("dc", 1, LivekitRtc.SignalTarget.PUBLISHER);
        builder.addDisabledTrack("TR_DIS");
        builder.setDataChannelReceiveState("PA_1", 1);
        
        builder.clear();
        LivekitRtc.SyncState syncState = builder.build();
        
        assertFalse(syncState.hasAnswer());
        assertFalse(syncState.hasSubscription());
        assertEquals(0, syncState.getPublishTracksCount());
        assertEquals(0, syncState.getDataChannelsCount());
        assertEquals(0, syncState.getTrackSidsDisabledCount());
        assertEquals(0, syncState.getDatachannelReceiveStatesCount());
    }

    @Test
    void testBuilderChaining() {
        LivekitRtc.SessionDescription answer = LivekitRtc.SessionDescription.newBuilder()
                .setType("answer")
                .setSdp("sdp")
                .build();
        
        LivekitRtc.SyncState syncState = new SyncStateBuilder()
                .setAnswer(answer)
                .addSubscribedTrack("TR_1")
                .addSubscribedTrack("TR_2")
                .addDisabledTrack("TR_DIS")
                .addDataChannel("dc", 0, LivekitRtc.SignalTarget.PUBLISHER)
                .build();
        
        assertTrue(syncState.hasAnswer());
        assertEquals(2, syncState.getSubscription().getTrackSidsCount());
        assertEquals(1, syncState.getTrackSidsDisabledCount());
        assertEquals(1, syncState.getDataChannelsCount());
    }
}
