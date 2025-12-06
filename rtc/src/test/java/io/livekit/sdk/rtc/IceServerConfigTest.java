package io.livekit.sdk.rtc;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class IceServerConfigTest {

    @Test
    void testDefaultConstructor() {
        IceServerConfig config = new IceServerConfig();
        assertTrue(config.getUrls().isEmpty());
        assertNull(config.getUsername());
        assertNull(config.getCredential());
    }

    @Test
    void testSingleUrlConstructor() {
        IceServerConfig config = new IceServerConfig("stun:stun.l.google.com:19302");
        
        assertEquals(1, config.getUrls().size());
        assertEquals("stun:stun.l.google.com:19302", config.getUrls().get(0));
    }

    @Test
    void testMultiUrlConstructor() {
        List<String> urls = Arrays.asList(
                "turn:turn1.example.com:3478",
                "turn:turn2.example.com:3478"
        );
        IceServerConfig config = new IceServerConfig(urls);
        
        assertEquals(2, config.getUrls().size());
    }

    @Test
    void testAddUrl() {
        IceServerConfig config = new IceServerConfig();
        config.addUrl("stun:stun.example.com");
        
        assertEquals(1, config.getUrls().size());
        assertEquals("stun:stun.example.com", config.getUrls().get(0));
    }

    @Test
    void testCredentials() {
        IceServerConfig config = new IceServerConfig("turn:turn.example.com");
        config.setUsername("user");
        config.setCredential("password");
        
        assertEquals("user", config.getUsername());
        assertEquals("password", config.getCredential());
    }
}
