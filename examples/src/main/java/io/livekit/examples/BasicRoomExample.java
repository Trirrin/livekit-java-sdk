package io.livekit.examples;

import io.livekit.sdk.ConnectionQuality;
import io.livekit.sdk.DataPacket;
import io.livekit.sdk.Participant;
import io.livekit.sdk.RemoteParticipant;
import io.livekit.sdk.Room;
import io.livekit.sdk.RoomListener;
import io.livekit.sdk.RoomOptions;
import io.livekit.sdk.Track;
import io.livekit.sdk.TrackPublication;
import io.livekit.sdk.signaling.LiveKitClient;

/**
 * Basic example showing how to join a LiveKit room and handle events.
 *
 * <p>Usage: Set LIVEKIT_URL and LIVEKIT_TOKEN environment variables, then run:
 *
 * <pre>./gradlew :examples:run</pre>
 */
public class BasicRoomExample {

  public static void main(String[] args) {
    String url = System.getenv("LIVEKIT_URL");
    String token = System.getenv("LIVEKIT_TOKEN");

    if (url == null || token == null) {
      System.err.println("Error: LIVEKIT_URL and LIVEKIT_TOKEN environment variables required");
      System.err.println("Example:");
      System.err.println("  export LIVEKIT_URL=wss://your-server.livekit.cloud");
      System.err.println("  export LIVEKIT_TOKEN=your-access-token");
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
            System.out.println("Local participant: " + r.getLocalParticipant().getIdentity());
          }

          @Override
          public void onDisconnected(Room r, io.livekit.sdk.DisconnectReason reason) {
            System.out.println("Disconnected from room: " + reason);
          }

          @Override
          public void onReconnecting(Room r) {
            System.out.println("Reconnecting...");
          }

          @Override
          public void onReconnected(Room r) {
            System.out.println("Reconnected!");
          }

          @Override
          public void onParticipantConnected(Room r, RemoteParticipant participant) {
            System.out.println("Participant joined: " + participant.getIdentity());
          }

          @Override
          public void onParticipantDisconnected(Room r, RemoteParticipant participant) {
            System.out.println("Participant left: " + participant.getIdentity());
          }

          @Override
          public void onTrackPublished(
              Room r, TrackPublication publication, Participant participant) {
            System.out.println(
                "Track published: " + publication.getSid() + " by " + participant.getIdentity());
          }

          @Override
          public void onTrackUnpublished(
              Room r, TrackPublication publication, Participant participant) {
            System.out.println(
                "Track unpublished: " + publication.getSid() + " by " + participant.getIdentity());
          }

          @Override
          public void onTrackSubscribed(
              Room r, Track track, TrackPublication publication, RemoteParticipant participant) {
            System.out.println(
                "Track subscribed: "
                    + track.getSid()
                    + " ("
                    + track.getType()
                    + ") from "
                    + participant.getIdentity());
          }

          @Override
          public void onTrackUnsubscribed(
              Room r, Track track, TrackPublication publication, RemoteParticipant participant) {
            System.out.println("Track unsubscribed: " + track.getSid());
          }

          @Override
          public void onTrackMuted(Room r, TrackPublication publication, Participant participant) {
            System.out.println(
                "Track muted: " + publication.getSid() + " by " + participant.getIdentity());
          }

          @Override
          public void onTrackUnmuted(
              Room r, TrackPublication publication, Participant participant) {
            System.out.println(
                "Track unmuted: " + publication.getSid() + " by " + participant.getIdentity());
          }

          @Override
          public void onConnectionQualityChanged(
              Room r, Participant participant, ConnectionQuality quality) {
            System.out.println(
                "Connection quality: " + quality + " for " + participant.getIdentity());
          }

          @Override
          public void onDataReceived(
              Room r,
              byte[] data,
              RemoteParticipant participant,
              DataPacket.Kind kind,
              String topic) {
            System.out.println(
                "Data received from "
                    + (participant != null ? participant.getIdentity() : "server")
                    + ": "
                    + new String(data));
          }
        });

    System.out.println("Connecting to " + url + "...");
    client.connect(url, token);

    // Keep the application running
    System.out.println("Press Ctrl+C to disconnect");
    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  System.out.println("Shutting down...");
                  client.disconnect();
                }));

    // Block main thread
    try {
      Thread.sleep(Long.MAX_VALUE);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
