package io.livekit.sdk;

import java.util.Collections;
import java.util.List;
import livekit.LivekitRtc;

/** Subscription permission granted to a single participant for local tracks. */
public class ParticipantTrackPermission {
  private final String participantIdentity;
  private final boolean allTracksAllowed;
  private final List<String> allowedTrackSids;

  /**
   * @param participantIdentity identity of the participant this permission applies to
   * @param allTracksAllowed when true, the participant may subscribe to all local tracks
   * @param allowedTrackSids track sids the participant may subscribe to; ignored when
   *     allTracksAllowed is true
   */
  public ParticipantTrackPermission(
      String participantIdentity, boolean allTracksAllowed, List<String> allowedTrackSids) {
    this.participantIdentity = participantIdentity;
    this.allTracksAllowed = allTracksAllowed;
    this.allowedTrackSids =
        allowedTrackSids == null ? Collections.emptyList() : List.copyOf(allowedTrackSids);
  }

  public String getParticipantIdentity() {
    return participantIdentity;
  }

  public boolean isAllTracksAllowed() {
    return allTracksAllowed;
  }

  public List<String> getAllowedTrackSids() {
    return allowedTrackSids;
  }

  public LivekitRtc.TrackPermission toProto() {
    return LivekitRtc.TrackPermission.newBuilder()
        .setParticipantIdentity(participantIdentity)
        .setAllTracks(allTracksAllowed)
        .addAllTrackSids(allowedTrackSids)
        .build();
  }
}
