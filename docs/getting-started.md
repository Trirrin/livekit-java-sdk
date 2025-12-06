# Getting Started

This guide will help you get started with the LiveKit Java SDK.

## Prerequisites

- Java 21 or higher
- A LiveKit server (self-hosted or [LiveKit Cloud](https://cloud.livekit.io))
- An access token (generated via [LiveKit CLI](https://github.com/livekit/livekit-cli) or server SDK)

## Installation

[![](https://jitpack.io/v/Trirrin/livekit-java-sdk.svg)](https://jitpack.io/#Trirrin/livekit-java-sdk)

### Gradle (Kotlin DSL)

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

// build.gradle.kts
dependencies {
    // Full SDK with WebRTC support
    implementation("com.github.Trirrin.livekit-java-sdk:rtc:v0.1.0")
    
    // Or signaling only (no audio/video)
    // implementation("com.github.Trirrin.livekit-java-sdk:signaling:v0.1.0")
}
```

### Gradle (Groovy DSL)

```groovy
// settings.gradle
dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}

// build.gradle
dependencies {
    implementation 'com.github.Trirrin.livekit-java-sdk:rtc:v0.1.0'
}
```

### Maven

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>com.github.Trirrin.livekit-java-sdk</groupId>
        <artifactId>rtc</artifactId>
        <version>v0.1.0</version>
    </dependency>
</dependencies>
```

## Basic Usage

### Connecting to a Room

```java
import io.livekit.sdk.Room;
import io.livekit.sdk.RoomOptions;
import io.livekit.sdk.RoomListener;
import io.livekit.sdk.signaling.LiveKitClient;

public class MyApp {
    public static void main(String[] args) {
        // Create client with default options
        RoomOptions options = new RoomOptions();
        LiveKitClient client = new LiveKitClient(options);
        
        // Get the room instance
        Room room = client.getRoom();
        
        // Add event listener
        room.addListener(new RoomListener() {
            @Override
            public void onConnected(Room room) {
                System.out.println("Connected to room: " + room.getName());
                System.out.println("My identity: " + room.getLocalParticipant().getIdentity());
            }
            
            @Override
            public void onDisconnected(Room room, DisconnectReason reason) {
                System.out.println("Disconnected: " + reason);
            }
        });
        
        // Connect to LiveKit server
        String url = "wss://your-server.livekit.cloud";
        String token = "your-access-token";
        client.connect(url, token);
    }
}
```

### Handling Participants

```java
room.addListener(new RoomListener() {
    @Override
    public void onParticipantConnected(Room room, RemoteParticipant participant) {
        System.out.println("Participant joined: " + participant.getIdentity());
        System.out.println("Participant name: " + participant.getName());
    }
    
    @Override
    public void onParticipantDisconnected(Room room, RemoteParticipant participant) {
        System.out.println("Participant left: " + participant.getIdentity());
    }
});

// Get all remote participants
Map<String, RemoteParticipant> participants = room.getRemoteParticipants();
for (RemoteParticipant p : participants.values()) {
    System.out.println("Participant: " + p.getIdentity());
}
```

### Handling Tracks

```java
room.addListener(new RoomListener() {
    @Override
    public void onTrackSubscribed(Room room, Track track, 
            TrackPublication publication, RemoteParticipant participant) {
        System.out.println("Subscribed to " + track.getType() + " from " + participant.getIdentity());
        
        if (track.getType() == TrackType.VIDEO) {
            // Handle video track
            VideoTrack videoTrack = (VideoTrack) track;
        } else if (track.getType() == TrackType.AUDIO) {
            // Handle audio track
            AudioTrack audioTrack = (AudioTrack) track;
        }
    }
    
    @Override
    public void onTrackMuted(Room room, TrackPublication publication, Participant participant) {
        System.out.println(participant.getIdentity() + " muted " + publication.getType());
    }
    
    @Override
    public void onTrackUnmuted(Room room, TrackPublication publication, Participant participant) {
        System.out.println(participant.getIdentity() + " unmuted " + publication.getType());
    }
});
```

### Connection Quality

```java
room.addListener(new RoomListener() {
    @Override
    public void onConnectionQualityChanged(Room room, Participant participant, ConnectionQuality quality) {
        System.out.println(participant.getIdentity() + " connection quality: " + quality);
        // ConnectionQuality: EXCELLENT, GOOD, POOR, LOST, UNKNOWN
    }
});
```

### Disconnecting

```java
// Gracefully disconnect
client.disconnect();

// Shutdown client and release resources
client.shutdown();
```

## RTC Client (Full Media Support)

For publishing and subscribing to media tracks, use `RtcClient` instead of `LiveKitClient`:

```java
import io.livekit.sdk.rtc.RtcClient;

RtcClient client = new RtcClient();
Room room = client.getRoom();

// Connect
client.connect(url, token).thenAccept(r -> {
    System.out.println("Connected with media support");
});
```

See [Publishing Media](publishing.md) for details on publishing audio/video tracks.

## Configuration Options

### RoomOptions

```java
RoomOptions options = new RoomOptions()
    .setAutoSubscribe(true)        // Auto-subscribe to tracks (default: true)
    .setAdaptiveStream(false)      // Adaptive streaming (default: false)
    .setDynacast(false)            // Dynamic broadcast (default: false)
    .setReconnectAttempts(5)       // Max reconnection attempts (default: 5)
    .setReconnectDelayMs(1000);    // Initial reconnect delay (default: 1000ms)
```

## Error Handling

```java
client.connect(url, token)
    .thenAccept(room -> {
        System.out.println("Connected successfully");
    })
    .exceptionally(e -> {
        System.err.println("Connection failed: " + e.getMessage());
        return null;
    });
```

## Next Steps

- [Publishing Media](publishing.md) - Learn to publish audio/video
- [Data Channels](data-channels.md) - Send arbitrary data
- [E2EE](e2ee.md) - End-to-end encryption
- [API Reference](api-reference.md) - Complete API documentation
