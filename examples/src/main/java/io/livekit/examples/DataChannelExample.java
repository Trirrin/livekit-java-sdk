package io.livekit.examples;

import io.livekit.sdk.DataPacket;
import io.livekit.sdk.DisconnectReason;
import io.livekit.sdk.RemoteParticipant;
import io.livekit.sdk.Room;
import io.livekit.sdk.RoomListener;
import io.livekit.sdk.RoomOptions;
import io.livekit.sdk.rtc.RtcClient;
import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Example showing how to use data channels for messaging in a LiveKit room.
 *
 * <p>Usage: Set LIVEKIT_URL and LIVEKIT_TOKEN environment variables, then run:
 *
 * <pre>./gradlew :examples:runDataChannel</pre>
 */
public class DataChannelExample {

  private static RtcClient client;

  public static void main(String[] args) {
    String url = System.getenv("LIVEKIT_URL");
    String token = System.getenv("LIVEKIT_TOKEN");

    if (url == null || token == null) {
      System.err.println("Error: LIVEKIT_URL and LIVEKIT_TOKEN environment variables required");
      System.exit(1);
    }

    RoomOptions options = new RoomOptions();
    client = new RtcClient(options);
    Room room = client.getRoom();

    room.addListener(
        new RoomListener() {
          @Override
          public void onConnected(Room r) {
            System.out.println("Connected to room: " + r.getName());
            System.out.println("Type messages and press Enter to send (reliable)");
            System.out.println("Prefix with '!' for unreliable (lossy) delivery");
            System.out.println("Type 'quit' to exit\n");
          }

          @Override
          public void onDisconnected(Room r, DisconnectReason reason) {
            System.out.println("Disconnected: " + reason);
          }

          @Override
          public void onParticipantConnected(Room r, RemoteParticipant participant) {
            System.out.println("[" + participant.getIdentity() + " joined]");
          }

          @Override
          public void onParticipantDisconnected(Room r, RemoteParticipant participant) {
            System.out.println("[" + participant.getIdentity() + " left]");
          }

          @Override
          public void onDataReceived(
              Room r,
              byte[] data,
              RemoteParticipant participant,
              DataPacket.Kind kind,
              String topic) {
            String sender = participant != null ? participant.getIdentity() : "server";
            String message = new String(data);
            String topicStr = topic != null ? " [" + topic + "]" : "";
            System.out.println("<" + sender + ">" + topicStr + " " + message);
          }
        });

    System.out.println("Connecting to " + url + "...");
    client.connect(url, token);

    // Start message input loop
    new Thread(DataChannelExample::messageLoop).start();

    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  System.out.println("\nShutting down...");
                  client.disconnect();
                }));

    try {
      Thread.sleep(Long.MAX_VALUE);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  private static void messageLoop() {
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
      String line;
      while ((line = reader.readLine()) != null) {
        if (line.equalsIgnoreCase("quit")) {
          System.exit(0);
        }

        if (line.isEmpty()) {
          continue;
        }

        boolean reliable = !line.startsWith("!");
        String message = reliable ? line : line.substring(1);

        DataPacket packet =
            DataPacket.builder(message.getBytes())
                .topic("chat")
                .kind(reliable ? DataPacket.Kind.RELIABLE : DataPacket.Kind.LOSSY)
                .build();

        client.publishData(packet);
        System.out.println("(sent " + (reliable ? "reliable" : "lossy") + ") " + message);
      }
    } catch (Exception e) {
      System.err.println("Error reading input: " + e.getMessage());
    }
  }
}
