# Publishing Media

This guide covers publishing audio and video tracks to a LiveKit room.

## Overview

To publish media, use `RtcClient` which provides full WebRTC integration. The SDK uses [webrtc-java](https://github.com/niclasvaneyk/webrtc-java) for native WebRTC support.

## Setup

```java
import io.livekit.sdk.Room;
import io.livekit.sdk.rtc.RtcClient;
import io.livekit.sdk.rtc.LocalAudioTrack;
import io.livekit.sdk.rtc.LocalVideoTrack;
import io.livekit.sdk.rtc.MediaDeviceInfo;

RtcClient client = new RtcClient();
client.connect(url, token).join();
```

## Device Enumeration

### List Available Devices

```java
// Audio input devices (microphones)
List<MediaDeviceInfo> microphones = client.getAudioInputDevices();
for (MediaDeviceInfo device : microphones) {
    System.out.println("Mic: " + device.getLabel() + " (id: " + device.getDeviceId() + ")");
}

// Audio output devices (speakers)
List<MediaDeviceInfo> speakers = client.getAudioOutputDevices();
for (MediaDeviceInfo device : speakers) {
    System.out.println("Speaker: " + device.getLabel());
}

// Video input devices (cameras)
List<MediaDeviceInfo> cameras = client.getVideoInputDevices();
for (MediaDeviceInfo device : cameras) {
    System.out.println("Camera: " + device.getLabel());
}
```

### MediaDeviceInfo

```java
public class MediaDeviceInfo {
    String getDeviceId();   // Device identifier
    String getLabel();      // Human-readable name
    Kind getKind();         // AUDIO_INPUT, AUDIO_OUTPUT, or VIDEO_INPUT
}
```

## Publishing Audio

### Using Default Microphone

```java
// Create audio track with default device
LocalAudioTrack audioTrack = client.createAudioTrack();
if (audioTrack != null) {
    client.publishAudioTrack(audioTrack);
}
```

### Using Specific Microphone

```java
// Get device list
List<MediaDeviceInfo> mics = client.getAudioInputDevices();
String deviceId = mics.get(0).getDeviceId();

// Create track with specific device
LocalAudioTrack audioTrack = client.createAudioTrack(deviceId, "my-mic");
client.publishAudioTrack(audioTrack);
```

## Publishing Video

### Using Default Camera

```java
// Create video track with default device (1280x720 @ 30fps)
LocalVideoTrack videoTrack = client.createVideoTrack();
if (videoTrack != null) {
    client.publishVideoTrack(videoTrack);
}
```

### Using Specific Camera and Resolution

```java
// Get camera list
List<MediaDeviceInfo> cameras = client.getVideoInputDevices();
String deviceId = cameras.get(0).getDeviceId();

// Create track with specific parameters
LocalVideoTrack videoTrack = client.createVideoTrack(
    deviceId,   // Device ID
    "my-cam",   // Track name
    1920,       // Width
    1080,       // Height
    30          // Frame rate
);
client.publishVideoTrack(videoTrack);
```

### Common Video Resolutions

| Preset | Resolution | Frame Rate | Use Case |
|--------|------------|------------|----------|
| 360p | 640x360 | 30 | Low bandwidth |
| 720p | 1280x720 | 30 | Standard HD |
| 1080p | 1920x1080 | 30 | Full HD |
| 4K | 3840x2160 | 30 | Ultra HD |

## Unpublishing Tracks

```java
// Unpublish by track ID
client.unpublishTrack(audioTrack.getSid());
client.unpublishTrack(videoTrack.getSid());
```

## LocalParticipant Controls

You can also control media through `LocalParticipant`:

```java
LocalParticipant local = room.getLocalParticipant();

// Enable/disable microphone
local.setMicrophoneEnabled(true);
local.setMicrophoneEnabled(false);

// Enable/disable camera
local.setCameraEnabled(true);
local.setCameraEnabled(false);
```

## Subscribing to Remote Tracks

Remote tracks are automatically subscribed when `autoSubscribe` is enabled (default). Handle them via `RoomListener`:

```java
room.addListener(new RoomListener() {
    @Override
    public void onTrackSubscribed(Room room, Track track, 
            TrackPublication publication, RemoteParticipant participant) {
        
        switch (track.getType()) {
            case AUDIO:
                handleAudioTrack((AudioTrack) track, participant);
                break;
            case VIDEO:
                handleVideoTrack((VideoTrack) track, participant);
                break;
        }
    }
    
    @Override
    public void onTrackUnsubscribed(Room room, Track track,
            TrackPublication publication, RemoteParticipant participant) {
        System.out.println("Track unsubscribed: " + track.getSid());
    }
});

void handleAudioTrack(AudioTrack track, RemoteParticipant participant) {
    System.out.println("Audio from: " + participant.getIdentity());
    // Audio plays automatically through default output device
}

void handleVideoTrack(VideoTrack track, RemoteParticipant participant) {
    System.out.println("Video from: " + participant.getIdentity());
    // Access native WebRTC track for rendering
    // RemoteVideoTrack has getNativeTrack() for integration with video renderers
}
```

## Track Types and Sources

### TrackType

```java
public enum TrackType {
    AUDIO,
    VIDEO,
    DATA
}
```

### TrackSource

```java
public enum TrackSource {
    UNKNOWN,
    CAMERA,
    MICROPHONE,
    SCREEN_SHARE,
    SCREEN_SHARE_AUDIO
}
```

## Track Publications

Track publications contain metadata about published tracks:

```java
TrackPublication pub = participant.getTrackPublicationBySource(TrackSource.CAMERA);
if (pub != null) {
    System.out.println("Track SID: " + pub.getSid());
    System.out.println("Track name: " + pub.getName());
    System.out.println("Track type: " + pub.getType());
    System.out.println("Is muted: " + pub.isMuted());
    System.out.println("Is subscribed: " + pub.isSubscribed());
    System.out.println("MIME type: " + pub.getMimeType());
}
```

## Known Limitations

- **Screen sharing**: webrtc-java supports `ScreenCapturer`/`WindowCapturer` but this is not yet exposed in the SDK
- **Simulcast/SVC**: Layer selection for adaptive streaming is not yet implemented
- **Audio processing**: Advanced audio options (noise suppression, echo cancellation) are not yet configurable

## Next Steps

- [Data Channels](data-channels.md) - Send arbitrary data
- [E2EE](e2ee.md) - Encrypt your tracks
- [API Reference](api-reference.md) - Complete API documentation
