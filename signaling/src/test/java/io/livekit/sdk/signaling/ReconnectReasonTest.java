package io.livekit.sdk.signaling;

import static org.junit.jupiter.api.Assertions.*;

import livekit.LivekitModels;
import org.junit.jupiter.api.Test;

class ReconnectReasonTest {

  @Test
  void testFromProto() {
    assertEquals(
        ReconnectReason.UNKNOWN,
        ReconnectReason.fromProto(LivekitModels.ReconnectReason.RR_UNKNOWN));
    assertEquals(
        ReconnectReason.SIGNAL_DISCONNECTED,
        ReconnectReason.fromProto(LivekitModels.ReconnectReason.RR_SIGNAL_DISCONNECTED));
    assertEquals(
        ReconnectReason.PUBLISHER_FAILED,
        ReconnectReason.fromProto(LivekitModels.ReconnectReason.RR_PUBLISHER_FAILED));
    assertEquals(
        ReconnectReason.SUBSCRIBER_FAILED,
        ReconnectReason.fromProto(LivekitModels.ReconnectReason.RR_SUBSCRIBER_FAILED));
    assertEquals(
        ReconnectReason.SWITCH_CANDIDATE,
        ReconnectReason.fromProto(LivekitModels.ReconnectReason.RR_SWITCH_CANDIDATE));
  }

  @Test
  void testToProto() {
    assertEquals(LivekitModels.ReconnectReason.RR_UNKNOWN, ReconnectReason.UNKNOWN.toProto());
    assertEquals(
        LivekitModels.ReconnectReason.RR_SIGNAL_DISCONNECTED,
        ReconnectReason.SIGNAL_DISCONNECTED.toProto());
    assertEquals(
        LivekitModels.ReconnectReason.RR_PUBLISHER_FAILED,
        ReconnectReason.PUBLISHER_FAILED.toProto());
    assertEquals(
        LivekitModels.ReconnectReason.RR_SUBSCRIBER_FAILED,
        ReconnectReason.SUBSCRIBER_FAILED.toProto());
    assertEquals(
        LivekitModels.ReconnectReason.RR_SWITCH_CANDIDATE,
        ReconnectReason.SWITCH_CANDIDATE.toProto());
  }

  @Test
  void testRoundTrip() {
    for (ReconnectReason reason : ReconnectReason.values()) {
      assertEquals(reason, ReconnectReason.fromProto(reason.toProto()));
    }
  }

  @Test
  void testUnrecognizedProtoValue() {
    // UNRECOGNIZED should map to UNKNOWN
    assertEquals(
        ReconnectReason.UNKNOWN,
        ReconnectReason.fromProto(LivekitModels.ReconnectReason.UNRECOGNIZED));
  }
}
