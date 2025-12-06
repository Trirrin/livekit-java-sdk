package io.livekit.sdk.rtc;

import dev.onvoid.webrtc.RTCIceCandidate;

/**
 * Utility class for parsing ICE candidates from/to JSON format.
 * LiveKit uses JSON-encoded strings for ICE candidate transport.
 */
public final class IceCandidateParser {

    private IceCandidateParser() {}

    /**
     * Parse an ICE candidate from a JSON string.
     * Format: {"candidate":"...","sdpMid":"...","sdpMLineIndex":0}
     */
    public static RTCIceCandidate parse(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }

        try {
            String candidate = extractString(json, "candidate");
            String sdpMid = extractString(json, "sdpMid");
            int sdpMLineIndex = extractInt(json, "sdpMLineIndex");

            if (candidate == null) {
                return null;
            }

            return new RTCIceCandidate(sdpMid, sdpMLineIndex, candidate);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Convert an ICE candidate to a JSON string.
     */
    public static String toJson(RTCIceCandidate candidate) {
        if (candidate == null) {
            return "{}";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"candidate\":\"").append(escapeJson(candidate.sdp)).append("\"");
        if (candidate.sdpMid != null) {
            sb.append(",\"sdpMid\":\"").append(escapeJson(candidate.sdpMid)).append("\"");
        }
        sb.append(",\"sdpMLineIndex\":").append(candidate.sdpMLineIndex);
        sb.append("}");
        return sb.toString();
    }

    private static String extractString(String json, String key) {
        String searchKey = "\"" + key + "\":";
        int keyIndex = json.indexOf(searchKey);
        if (keyIndex < 0) {
            return null;
        }

        int valueStart = keyIndex + searchKey.length();
        // Skip whitespace
        while (valueStart < json.length() && Character.isWhitespace(json.charAt(valueStart))) {
            valueStart++;
        }

        if (valueStart >= json.length()) {
            return null;
        }

        char startChar = json.charAt(valueStart);
        if (startChar == '"') {
            // String value
            int valueEnd = findClosingQuote(json, valueStart + 1);
            if (valueEnd < 0) {
                return null;
            }
            return unescapeJson(json.substring(valueStart + 1, valueEnd));
        } else if (startChar == 'n') {
            // null value
            return null;
        }

        return null;
    }

    private static int extractInt(String json, String key) {
        String searchKey = "\"" + key + "\":";
        int keyIndex = json.indexOf(searchKey);
        if (keyIndex < 0) {
            return 0;
        }

        int valueStart = keyIndex + searchKey.length();
        while (valueStart < json.length() && Character.isWhitespace(json.charAt(valueStart))) {
            valueStart++;
        }

        int valueEnd = valueStart;
        while (valueEnd < json.length() && 
               (Character.isDigit(json.charAt(valueEnd)) || json.charAt(valueEnd) == '-')) {
            valueEnd++;
        }

        if (valueEnd == valueStart) {
            return 0;
        }

        try {
            return Integer.parseInt(json.substring(valueStart, valueEnd));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static int findClosingQuote(String str, int start) {
        for (int i = start; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == '\\' && i + 1 < str.length()) {
                i++; // Skip escaped character
            } else if (c == '"') {
                return i;
            }
        }
        return -1;
    }

    private static String escapeJson(String str) {
        if (str == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String unescapeJson(String str) {
        if (str == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == '\\' && i + 1 < str.length()) {
                char next = str.charAt(++i);
                switch (next) {
                    case '"':
                        sb.append('"');
                        break;
                    case '\\':
                        sb.append('\\');
                        break;
                    case 'n':
                        sb.append('\n');
                        break;
                    case 'r':
                        sb.append('\r');
                        break;
                    case 't':
                        sb.append('\t');
                        break;
                    default:
                        sb.append(next);
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
