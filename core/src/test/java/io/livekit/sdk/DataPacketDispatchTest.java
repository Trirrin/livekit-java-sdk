package io.livekit.sdk;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import livekit.LivekitModels;
import livekit.LivekitRtc;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Tests dispatch of transcription, chat, and SIP DTMF data packets to room events. */
class DataPacketDispatchTest {

  private Room room;
  private RoomTransportTest.FakeTransport transport;

  @BeforeEach
  void setUp() {
    room = new Room();
    transport = new RoomTransportTest.FakeTransport();
    room.setTransport(transport);
    room.handleJoinResponse(
        LivekitRtc.JoinResponse.newBuilder()
            .setRoom(LivekitModels.Room.newBuilder().setSid("RM_1"))
            .setParticipant(
                LivekitModels.ParticipantInfo.newBuilder().setSid("PA_LOCAL").setIdentity("local"))
            .build());
    room.handleParticipantUpdate(
        LivekitRtc.ParticipantUpdate.newBuilder()
            .addParticipants(
                LivekitModels.ParticipantInfo.newBuilder()
                    .setSid("PA_R1")
                    .setIdentity("remote1")
                    .addTracks(
                        LivekitModels.TrackInfo.newBuilder()
                            .setSid("TR_A1")
                            .setType(LivekitModels.TrackType.AUDIO)))
            .build());
  }

  @Test
  void transcriptionPacketFiresEvent() {
    AtomicReference<Participant> participant = new AtomicReference<>();
    AtomicReference<TrackPublication> publication = new AtomicReference<>();
    AtomicReference<List<TranscriptionSegment>> segments = new AtomicReference<>();
    room.addListener(
        new RoomListener() {
          @Override
          public void onTranscriptionReceived(
              Room r, Participant p, TrackPublication pub, List<TranscriptionSegment> segs) {
            participant.set(p);
            publication.set(pub);
            segments.set(segs);
          }
        });

    room.handleDataPacket(
        LivekitModels.DataPacket.newBuilder()
            .setParticipantIdentity("remote1")
            .setTranscription(
                LivekitModels.Transcription.newBuilder()
                    .setTranscribedParticipantIdentity("remote1")
                    .setTrackId("TR_A1")
                    .addSegments(
                        LivekitModels.TranscriptionSegment.newBuilder()
                            .setId("seg-1")
                            .setText("hello world")
                            .setFinal(true)
                            .setLanguage("en")))
            .build(),
        DataPacket.Kind.RELIABLE);

    assertEquals("remote1", participant.get().getIdentity());
    assertEquals("TR_A1", publication.get().getSid());
    assertEquals(1, segments.get().size());
    assertEquals("hello world", segments.get().get(0).getText());
    assertTrue(segments.get().get(0).isFinal());
  }

  @Test
  void chatMessagePacketFiresEvent() {
    AtomicReference<ChatMessage> received = new AtomicReference<>();
    AtomicReference<RemoteParticipant> from = new AtomicReference<>();
    room.addListener(
        new RoomListener() {
          @Override
          public void onChatMessageReceived(Room r, ChatMessage message, RemoteParticipant sender) {
            received.set(message);
            from.set(sender);
          }
        });

    room.handleDataPacket(
        LivekitModels.DataPacket.newBuilder()
            .setParticipantIdentity("remote1")
            .setChatMessage(
                LivekitModels.ChatMessage.newBuilder()
                    .setId("msg-1")
                    .setTimestamp(1234L)
                    .setMessage("hi there"))
            .build(),
        DataPacket.Kind.RELIABLE);

    assertEquals("hi there", received.get().getMessage());
    assertEquals("msg-1", received.get().getId());
    assertEquals("remote1", from.get().getIdentity());
  }

  @Test
  void sipDtmfPacketFiresEvent() {
    AtomicReference<String> digit = new AtomicReference<>();
    room.addListener(
        new RoomListener() {
          @Override
          public void onSipDtmfReceived(Room r, RemoteParticipant sender, int code, String d) {
            digit.set(d);
          }
        });

    room.handleDataPacket(
        LivekitModels.DataPacket.newBuilder()
            .setParticipantIdentity("remote1")
            .setSipDtmf(LivekitModels.SipDTMF.newBuilder().setCode(5).setDigit("5"))
            .build(),
        DataPacket.Kind.RELIABLE);

    assertEquals("5", digit.get());
  }

  @Test
  void sendChatMessageSendsReliablePacket() {
    ChatMessage sent = room.getLocalParticipant().sendChatMessage("hello chat");

    assertNotNull(transport.lastDataPacket);
    assertTrue(transport.lastDataReliable);
    assertEquals("hello chat", transport.lastDataPacket.getChatMessage().getMessage());
    assertEquals(sent.getId(), transport.lastDataPacket.getChatMessage().getId());

    ChatMessage edited = room.getLocalParticipant().editChatMessage("fixed", sent);
    assertEquals(sent.getId(), edited.getId());
    assertNotNull(edited.getEditTimestamp());
    assertTrue(transport.lastDataPacket.getChatMessage().hasEditTimestamp());
  }
}
