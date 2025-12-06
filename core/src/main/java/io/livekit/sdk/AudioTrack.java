package io.livekit.sdk;

/**
 * Represents an audio track.
 */
public class AudioTrack extends Track {

    public AudioTrack(String sid, String name) {
        super(sid, name, TrackType.AUDIO);
    }
}
