package io.livekit.sdk;

/**
 * Represents a data track for sending arbitrary data.
 */
public class DataTrack extends Track {

    public DataTrack(String sid, String name) {
        super(sid, name, TrackType.DATA);
    }
}
