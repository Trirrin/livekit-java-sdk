package io.livekit.sdk.rpc;

/** Data passed to an RPC method handler when it is invoked. */
public class RpcInvocationData {
  private final String requestId;
  private final String callerIdentity;
  private final String payload;
  private final long responseTimeoutMs;

  public RpcInvocationData(
      String requestId, String callerIdentity, String payload, long responseTimeoutMs) {
    this.requestId = requestId;
    this.callerIdentity = callerIdentity;
    this.payload = payload;
    this.responseTimeoutMs = responseTimeoutMs;
  }

  public String getRequestId() {
    return requestId;
  }

  /** Identity of the participant that invoked this method. */
  public String getCallerIdentity() {
    return callerIdentity;
  }

  /** Request payload, at most 15KiB of UTF-8 text. */
  public String getPayload() {
    return payload;
  }

  /** Time remaining for the handler to return before the caller times out. */
  public long getResponseTimeoutMs() {
    return responseTimeoutMs;
  }
}
