package io.livekit.examples;

import io.livekit.sdk.DisconnectReason;
import io.livekit.sdk.Room;
import io.livekit.sdk.RoomListener;
import io.livekit.sdk.RoomOptions;
import io.livekit.sdk.rtc.MediaDeviceInfo;
import io.livekit.sdk.rtc.RtcClient;
import java.util.List;

/**
 * Example showing how to publish audio/video tracks to a LiveKit room.
 *
 * <p>Usage: Set LIVEKIT_URL and LIVEKIT_TOKEN environment variables, then run:
 *
 * <pre>./gradlew :examples:runPublish</pre>
 */
public class PublishExample {

  public static void main(String[] args) {
    String url = System.getenv("LIVEKIT_URL");
    String token = System.getenv("LIVEKIT_TOKEN");

    if (url == null || token == null) {
      System.err.println("Error: LIVEKIT_URL and LIVEKIT_TOKEN environment variables required");
      System.exit(1);
    }

    RoomOptions options = new RoomOptions();
    RtcClient client = new RtcClient(options);
    Room room = client.getRoom();

    room.addListener(
        new RoomListener() {
          @Override
          public void onConnected(Room r) {
            System.out.println("Connected to room: " + r.getName());
            publishTracks(client);
          }

          @Override
          public void onDisconnected(Room r, DisconnectReason reason) {
            System.out.println("Disconnected: " + reason);
          }
        });

    System.out.println("Connecting to " + url + "...");
    client.connect(url, token);

    // List available devices
    listDevices(client);

    System.out.println("Press Ctrl+C to disconnect");
    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  System.out.println("Shutting down...");
                  client.disconnect();
                }));

    try {
      Thread.sleep(Long.MAX_VALUE);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  private static void listDevices(RtcClient client) {
    System.out.println("\n=== Available Audio Devices ===");
    List<MediaDeviceInfo> audioDevices = client.getAudioInputDevices();
    for (MediaDeviceInfo device : audioDevices) {
      System.out.println("  " + device.getLabel() + " [" + device.getDeviceId() + "]");
    }

    System.out.println("\n=== Available Video Devices ===");
    List<MediaDeviceInfo> videoDevices = client.getVideoInputDevices();
    for (MediaDeviceInfo device : videoDevices) {
      System.out.println("  " + device.getLabel() + " [" + device.getDeviceId() + "]");
    }
    System.out.println();
  }

  private static void publishTracks(RtcClient client) {
    try {
      // Get default audio device
      List<MediaDeviceInfo> audioDevices = client.getAudioInputDevices();
      if (!audioDevices.isEmpty()) {
        MediaDeviceInfo defaultAudio = audioDevices.get(0);
        System.out.println("Publishing audio from: " + defaultAudio.getLabel());
        // client.createAudioTrack(defaultAudio.getDeviceId(), "microphone"); // Uncomment when
        // ready
      }

      // Get default video device
      List<MediaDeviceInfo> videoDevices = client.getVideoInputDevices();
      if (!videoDevices.isEmpty()) {
        MediaDeviceInfo defaultVideo = videoDevices.get(0);
        System.out.println("Publishing video from: " + defaultVideo.getLabel());
        // client.createVideoTrack(defaultVideo.getDeviceId(), "camera", 1280, 720, 30); //
        // Uncomment when ready
      }

      System.out.println("Track publishing configured (uncomment actual publish calls)");
    } catch (Exception e) {
      System.err.println("Error publishing tracks: " + e.getMessage());
    }
  }
}
