package io.livekit.sdk;

/** A segment of transcribed speech received from an agent or SIP participant. */
public class TranscriptionSegment {
  private final String id;
  private final String text;
  private final long startTime;
  private final long endTime;
  private final boolean isFinal;
  private final String language;

  public TranscriptionSegment(
      String id, String text, long startTime, long endTime, boolean isFinal, String language) {
    this.id = id;
    this.text = text;
    this.startTime = startTime;
    this.endTime = endTime;
    this.isFinal = isFinal;
    this.language = language;
  }

  public String getId() {
    return id;
  }

  public String getText() {
    return text;
  }

  public long getStartTime() {
    return startTime;
  }

  public long getEndTime() {
    return endTime;
  }

  /** False while the segment is still being refined by the transcriber. */
  public boolean isFinal() {
    return isFinal;
  }

  public String getLanguage() {
    return language;
  }

  public static TranscriptionSegment fromProto(livekit.LivekitModels.TranscriptionSegment proto) {
    return new TranscriptionSegment(
        proto.getId(),
        proto.getText(),
        proto.getStartTime(),
        proto.getEndTime(),
        proto.getFinal(),
        proto.getLanguage());
  }
}
