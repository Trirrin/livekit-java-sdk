package io.livekit.sdk;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LocalParticipantTest {

  private LocalParticipant participant;
  private TestTrackManager trackManager;

  @BeforeEach
  void setUp() {
    participant = new LocalParticipant("sid123", "user1");
    trackManager = new TestTrackManager();
  }

  @Test
  void testSetMicrophoneEnabled_withNoTrackManager() {
    // Should not throw even without track manager
    participant.setMicrophoneEnabled(true);
    assertFalse(participant.isMicrophoneEnabled());
  }

  @Test
  void testSetMicrophoneEnabled_withTrackManager() {
    participant.setTrackManager(trackManager);

    participant.setMicrophoneEnabled(true);
    assertTrue(trackManager.microphoneEnabled);

    participant.setMicrophoneEnabled(false);
    assertFalse(trackManager.microphoneEnabled);
  }

  @Test
  void testSetCameraEnabled_withTrackManager() {
    participant.setTrackManager(trackManager);

    participant.setCameraEnabled(true);
    assertTrue(trackManager.cameraEnabled);

    participant.setCameraEnabled(false);
    assertFalse(trackManager.cameraEnabled);
  }

  @Test
  void testSetScreenShareEnabled_withTrackManager() {
    participant.setTrackManager(trackManager);

    participant.setScreenShareEnabled(true);
    assertTrue(trackManager.screenShareEnabled);

    participant.setScreenShareEnabled(false);
    assertFalse(trackManager.screenShareEnabled);
  }

  @Test
  void testIsMicrophoneEnabled() {
    assertFalse(participant.isMicrophoneEnabled());

    participant.setTrackManager(trackManager);
    trackManager.microphoneEnabled = true;
    assertTrue(participant.isMicrophoneEnabled());
  }

  @Test
  void testIsCameraEnabled() {
    assertFalse(participant.isCameraEnabled());

    participant.setTrackManager(trackManager);
    trackManager.cameraEnabled = true;
    assertTrue(participant.isCameraEnabled());
  }

  @Test
  void testIsScreenShareEnabled() {
    assertFalse(participant.isScreenShareEnabled());

    participant.setTrackManager(trackManager);
    trackManager.screenShareEnabled = true;
    assertTrue(participant.isScreenShareEnabled());
  }

  @Test
  void testGetTrackManager() {
    assertNull(participant.getTrackManager());
    participant.setTrackManager(trackManager);
    assertSame(trackManager, participant.getTrackManager());
  }

  private static class TestTrackManager implements LocalTrackManager {
    boolean microphoneEnabled = false;
    boolean cameraEnabled = false;
    boolean screenShareEnabled = false;

    @Override
    public void setMicrophoneEnabled(boolean enabled) {
      microphoneEnabled = enabled;
    }

    @Override
    public void setCameraEnabled(boolean enabled) {
      cameraEnabled = enabled;
    }

    @Override
    public void setScreenShareEnabled(boolean enabled) {
      screenShareEnabled = enabled;
    }

    @Override
    public boolean isMicrophoneEnabled() {
      return microphoneEnabled;
    }

    @Override
    public boolean isCameraEnabled() {
      return cameraEnabled;
    }

    @Override
    public boolean isScreenShareEnabled() {
      return screenShareEnabled;
    }
  }
}
