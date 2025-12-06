package io.livekit.examples;

import io.livekit.sdk.DisconnectReason;
import io.livekit.sdk.Participant;
import io.livekit.sdk.RemoteParticipant;
import io.livekit.sdk.Room;
import io.livekit.sdk.RoomListener;
import io.livekit.sdk.RoomOptions;
import io.livekit.sdk.Track;
import io.livekit.sdk.TrackPublication;
import io.livekit.sdk.TrackType;
import io.livekit.sdk.signaling.LiveKitClient;

/**
 * Example showing how to subscribe to remote tracks in a LiveKit room.
 *
 * <p>Usage: Set LIVEKIT_URL and LIVEKIT_TOKEN environment variables, then run:
 *
 * <pre>./gradlew :examples:runSubscribe</pre>
 */
public class SubscribeExample {

  public static void main(String[] args) {
    String url = System.getenv("LIVEKIT_URL");
    String token = System.getenv("LIVEKIT_TOKEN");

    if (url == null || token == null) {
      System.err.println("Error: LIVEKIT_URL and LIVEKIT_TOKEN environment variables required");
      System.exit(1);
    }

    RoomOptions options = new RoomOptions();
    LiveKitClient client = new LiveKitClient(options);
    Room room = client.getRoom();

    room.addListener(
        new RoomListener() {
          @Override
          public void onConnected(Room r) {
            System.out.println("Connected to room: " + r.getName());
            System.out.println("Waiting for remote tracks...");

            // Check existing participants
            for (RemoteParticipant participant : r.getRemoteParticipants().values()) {
              System.out.println("Existing participant: " + participant.getIdentity());
              for (TrackPublication pub : participant.getTrackPublications().values()) {
                System.out.println(
                    "  Track: " + pub.getSid() + " (" + pub.getType() + ") - subscribed");
              }
            }
          }

          @Override
          public void onDisconnected(Room r, DisconnectReason reason) {
            System.out.println("Disconnected: " + reason);
          }

          @Override
          public void onParticipantConnected(Room r, RemoteParticipant participant) {
            System.out.println("Participant connected: " + participant.getIdentity());
          }

          @Override
          public void onTrackSubscribed(
              Room r, Track track, TrackPublication publication, RemoteParticipant participant) {
            System.out.println(
                "\n=== Track Subscribed ===\n"
                    + "  Participant: "
                    + participant.getIdentity()
                    + "\n"
                    + "  Track SID: "
                    + track.getSid()
                    + "\n"
                    + "  Kind: "
                    + track.getType()
                    + "\n"
                    + "  Source: "
                    + publication.getSource());

            if (track.getType() == TrackType.AUDIO) {
              handleAudioTrack(track);
            } else if (track.getType() == TrackType.VIDEO) {
              handleVideoTrack(track);
            }
          }

          @Override
          public void onTrackUnsubscribed(
              Room r, Track track, TrackPublication publication, RemoteParticipant participant) {
            System.out.println(
                "Track unsubscribed: " + track.getSid() + " from " + participant.getIdentity());
          }

          @Override
          public void onTrackMuted(Room r, TrackPublication publication, Participant participant) {
            System.out.println(
                "Track muted: " + publication.getSid() + " (" + participant.getIdentity() + ")");
          }

          @Override
          public void onTrackUnmuted(
              Room r, TrackPublication publication, Participant participant) {
            System.out.println(
                "Track unmuted: " + publication.getSid() + " (" + participant.getIdentity() + ")");
          }
        });

    System.out.println("Connecting to " + url + "...");
    client.connect(url, token);

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

  private static void handleAudioTrack(Track track) {
    System.out.println("Handling audio track: " + track.getSid());
    // Audio playback would be implemented here using javax.sound.sampled
    // or another audio library
  }

  private static void handleVideoTrack(Track track) {
    System.out.println("Handling video track: " + track.getSid());
    // Video rendering would be implemented here using Swing/JavaFX/AWT
    // or an external video renderer
  }
}
