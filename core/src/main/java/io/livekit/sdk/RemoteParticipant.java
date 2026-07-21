package io.livekit.sdk;

/** Represents a remote participant in the room. */
public class RemoteParticipant extends Participant {

  public RemoteParticipant(String sid, String identity) {
    super(sid, identity);
  }

  /** Subscribe to a remote track publication. */
  public void subscribe(TrackPublication publication) {
    if (publication instanceof RemoteTrackPublication) {
      ((RemoteTrackPublication) publication).setSubscribed(true);
    }
  }

  /** Unsubscribe from a remote track publication. */
  public void unsubscribe(TrackPublication publication) {
    if (publication instanceof RemoteTrackPublication) {
      ((RemoteTrackPublication) publication).setSubscribed(false);
    }
  }

  /**
   * @deprecated Subscription permissions are controlled by the publishing participant; use {@link
   *     LocalParticipant#setTrackSubscriptionPermissions}.
   */
  @Deprecated
  public void setSubscriptionPermissions(boolean allowed) {}
}
