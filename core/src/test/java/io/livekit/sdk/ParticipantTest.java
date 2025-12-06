package io.livekit.sdk;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ParticipantTest {

    @Test
    void testLocalParticipantCreation() {
        LocalParticipant participant = new LocalParticipant("sid123", "user1");
        assertEquals("sid123", participant.getSid());
        assertEquals("user1", participant.getIdentity());
        assertEquals(ConnectionQuality.UNKNOWN, participant.getConnectionQuality());
    }

    @Test
    void testRemoteParticipantCreation() {
        RemoteParticipant participant = new RemoteParticipant("sid456", "user2");
        assertEquals("sid456", participant.getSid());
        assertEquals("user2", participant.getIdentity());
    }

    @Test
    void testParticipantMetadata() {
        LocalParticipant participant = new LocalParticipant("sid", "identity");
        participant.setMetadata("{\"role\": \"admin\"}");
        assertEquals("{\"role\": \"admin\"}", participant.getMetadata());
    }

    @Test
    void testParticipantAttributes() {
        LocalParticipant participant = new LocalParticipant("sid", "identity");
        Map<String, String> attrs = new HashMap<>();
        attrs.put("key1", "value1");
        attrs.put("key2", "value2");
        
        participant.setAttributes(attrs);
        assertEquals("value1", participant.getAttributes().get("key1"));
        assertEquals(2, participant.getAttributes().size());
    }

    @Test
    void testTrackPublications() {
        LocalParticipant participant = new LocalParticipant("sid", "identity");
        TrackPublication pub = new TrackPublication("track_sid", "camera", TrackType.VIDEO);
        pub.setSource(TrackSource.CAMERA);
        
        participant.addTrackPublication(pub);
        
        assertEquals(pub, participant.getTrackPublication("track_sid"));
        assertEquals(pub, participant.getTrackPublicationBySource(TrackSource.CAMERA));
        assertEquals(1, participant.getTrackPublications().size());
        
        participant.removeTrackPublication("track_sid");
        assertTrue(participant.getTrackPublications().isEmpty());
    }
}
