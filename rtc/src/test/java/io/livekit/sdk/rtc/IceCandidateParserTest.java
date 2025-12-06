package io.livekit.sdk.rtc;

import static org.junit.jupiter.api.Assertions.*;

import dev.onvoid.webrtc.RTCIceCandidate;
import org.junit.jupiter.api.Test;

class IceCandidateParserTest {

  @Test
  void testParseValidCandidate() {
    String json =
        "{\"candidate\":\"candidate:123 1 udp 2113937151 192.168.1.1 54321 typ host\",\"sdpMid\":\"0\",\"sdpMLineIndex\":0}";

    RTCIceCandidate candidate = IceCandidateParser.parse(json);

    assertNotNull(candidate);
    assertEquals("candidate:123 1 udp 2113937151 192.168.1.1 54321 typ host", candidate.sdp);
    assertEquals("0", candidate.sdpMid);
    assertEquals(0, candidate.sdpMLineIndex);
  }

  @Test
  void testParseWithDifferentSdpMLineIndex() {
    String json = "{\"candidate\":\"candidate:456\",\"sdpMid\":\"audio\",\"sdpMLineIndex\":1}";

    RTCIceCandidate candidate = IceCandidateParser.parse(json);

    assertNotNull(candidate);
    assertEquals("candidate:456", candidate.sdp);
    assertEquals("audio", candidate.sdpMid);
    assertEquals(1, candidate.sdpMLineIndex);
  }

  @Test
  void testParseNullJson() {
    assertNull(IceCandidateParser.parse(null));
  }

  @Test
  void testParseEmptyJson() {
    assertNull(IceCandidateParser.parse(""));
  }

  @Test
  void testParseInvalidJson() {
    assertNull(IceCandidateParser.parse("not json"));
  }

  @Test
  void testParseMissingCandidate() {
    String json = "{\"sdpMid\":\"0\",\"sdpMLineIndex\":0}";
    assertNull(IceCandidateParser.parse(json));
  }

  @Test
  void testToJson() {
    RTCIceCandidate candidate = new RTCIceCandidate("video", 1, "candidate:789 1 tcp");

    String json = IceCandidateParser.toJson(candidate);

    assertTrue(json.contains("\"candidate\":\"candidate:789 1 tcp\""));
    assertTrue(json.contains("\"sdpMid\":\"video\""));
    assertTrue(json.contains("\"sdpMLineIndex\":1"));
  }

  @Test
  void testToJsonNull() {
    assertEquals("{}", IceCandidateParser.toJson(null));
  }

  @Test
  void testRoundTrip() {
    RTCIceCandidate original =
        new RTCIceCandidate(
            "0", 0, "candidate:123 1 udp 2113937151 192.168.1.1 54321 typ host generation 0");

    String json = IceCandidateParser.toJson(original);
    RTCIceCandidate parsed = IceCandidateParser.parse(json);

    assertNotNull(parsed);
    assertEquals(original.sdp, parsed.sdp);
    assertEquals(original.sdpMid, parsed.sdpMid);
    assertEquals(original.sdpMLineIndex, parsed.sdpMLineIndex);
  }

  @Test
  void testEscapedCharacters() {
    RTCIceCandidate candidate = new RTCIceCandidate("test\"mid", 0, "test\\candidate");

    String json = IceCandidateParser.toJson(candidate);
    RTCIceCandidate parsed = IceCandidateParser.parse(json);

    assertNotNull(parsed);
    assertEquals("test\\candidate", parsed.sdp);
    assertEquals("test\"mid", parsed.sdpMid);
  }
}
