package io.livekit.sdk;

import livekit.LivekitModels;
import livekit.LivekitRtc;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ProtoConverterTest {

    @Test
    void testTrackTypeConversion() {
        assertEquals(TrackType.AUDIO, ProtoConverter.fromProto(LivekitModels.TrackType.AUDIO));
        assertEquals(TrackType.VIDEO, ProtoConverter.fromProto(LivekitModels.TrackType.VIDEO));
        assertEquals(TrackType.DATA, ProtoConverter.fromProto(LivekitModels.TrackType.DATA));

        assertEquals(LivekitModels.TrackType.AUDIO, ProtoConverter.toProto(TrackType.AUDIO));
        assertEquals(LivekitModels.TrackType.VIDEO, ProtoConverter.toProto(TrackType.VIDEO));
        assertEquals(LivekitModels.TrackType.DATA, ProtoConverter.toProto(TrackType.DATA));
    }

    @Test
    void testTrackSourceConversion() {
        assertEquals(TrackSource.CAMERA, ProtoConverter.fromProto(LivekitModels.TrackSource.CAMERA));
        assertEquals(TrackSource.MICROPHONE, ProtoConverter.fromProto(LivekitModels.TrackSource.MICROPHONE));
        assertEquals(TrackSource.SCREEN_SHARE, ProtoConverter.fromProto(LivekitModels.TrackSource.SCREEN_SHARE));
        assertEquals(TrackSource.SCREEN_SHARE_AUDIO, ProtoConverter.fromProto(LivekitModels.TrackSource.SCREEN_SHARE_AUDIO));
        assertEquals(TrackSource.UNKNOWN, ProtoConverter.fromProto(LivekitModels.TrackSource.UNKNOWN));

        assertEquals(LivekitModels.TrackSource.CAMERA, ProtoConverter.toProto(TrackSource.CAMERA));
        assertEquals(LivekitModels.TrackSource.MICROPHONE, ProtoConverter.toProto(TrackSource.MICROPHONE));
        assertEquals(LivekitModels.TrackSource.SCREEN_SHARE, ProtoConverter.toProto(TrackSource.SCREEN_SHARE));
        assertEquals(LivekitModels.TrackSource.SCREEN_SHARE_AUDIO, ProtoConverter.toProto(TrackSource.SCREEN_SHARE_AUDIO));
        assertEquals(LivekitModels.TrackSource.UNKNOWN, ProtoConverter.toProto(TrackSource.UNKNOWN));
    }

    @Test
    void testConnectionQualityConversion() {
        assertEquals(ConnectionQuality.POOR, ProtoConverter.fromProto(LivekitModels.ConnectionQuality.POOR));
        assertEquals(ConnectionQuality.GOOD, ProtoConverter.fromProto(LivekitModels.ConnectionQuality.GOOD));
        assertEquals(ConnectionQuality.EXCELLENT, ProtoConverter.fromProto(LivekitModels.ConnectionQuality.EXCELLENT));
        assertEquals(ConnectionQuality.LOST, ProtoConverter.fromProto(LivekitModels.ConnectionQuality.LOST));
    }

    @Test
    void testDisconnectReasonConversion() {
        assertEquals(DisconnectReason.CLIENT_INITIATED, ProtoConverter.fromProto(LivekitModels.DisconnectReason.CLIENT_INITIATED));
        assertEquals(DisconnectReason.DUPLICATE_IDENTITY, ProtoConverter.fromProto(LivekitModels.DisconnectReason.DUPLICATE_IDENTITY));
        assertEquals(DisconnectReason.SERVER_SHUTDOWN, ProtoConverter.fromProto(LivekitModels.DisconnectReason.SERVER_SHUTDOWN));
        assertEquals(DisconnectReason.ROOM_DELETED, ProtoConverter.fromProto(LivekitModels.DisconnectReason.ROOM_DELETED));
    }

    @Test
    void testLocalParticipantFromProto() {
        LivekitModels.ParticipantInfo info = LivekitModels.ParticipantInfo.newBuilder()
                .setSid("PA_123")
                .setIdentity("user1")
                .setName("Test User")
                .setMetadata("{\"role\":\"host\"}")
                .putAttributes("key1", "value1")
                .build();

        LocalParticipant participant = ProtoConverter.localParticipantFromProto(info);

        assertEquals("PA_123", participant.getSid());
        assertEquals("user1", participant.getIdentity());
        assertEquals("Test User", participant.getName());
        assertEquals("{\"role\":\"host\"}", participant.getMetadata());
        assertEquals("value1", participant.getAttributes().get("key1"));
    }

    @Test
    void testRemoteParticipantFromProto() {
        LivekitModels.ParticipantInfo info = LivekitModels.ParticipantInfo.newBuilder()
                .setSid("PA_456")
                .setIdentity("user2")
                .setName("Remote User")
                .build();

        RemoteParticipant participant = ProtoConverter.remoteParticipantFromProto(info);

        assertEquals("PA_456", participant.getSid());
        assertEquals("user2", participant.getIdentity());
        assertEquals("Remote User", participant.getName());
    }

    @Test
    void testTrackPublicationFromProto() {
        LivekitModels.TrackInfo info = LivekitModels.TrackInfo.newBuilder()
                .setSid("TR_123")
                .setName("camera")
                .setType(LivekitModels.TrackType.VIDEO)
                .setSource(LivekitModels.TrackSource.CAMERA)
                .setMuted(false)
                .setMimeType("video/vp8")
                .build();

        TrackPublication publication = ProtoConverter.trackPublicationFromProto(info);

        assertEquals("TR_123", publication.getSid());
        assertEquals("camera", publication.getName());
        assertEquals(TrackType.VIDEO, publication.getType());
        assertEquals(TrackSource.CAMERA, publication.getSource());
        assertFalse(publication.isMuted());
        assertEquals("video/vp8", publication.getMimeType());
    }

    @Test
    void testBuildMetadataUpdate() {
        Map<String, String> attrs = new HashMap<>();
        attrs.put("attr1", "val1");

        LivekitRtc.UpdateParticipantMetadata update = 
                ProtoConverter.buildMetadataUpdate("test-meta", "Test Name", attrs);

        assertEquals("test-meta", update.getMetadata());
        assertEquals("Test Name", update.getName());
        assertEquals("val1", update.getAttributesMap().get("attr1"));
    }
}
