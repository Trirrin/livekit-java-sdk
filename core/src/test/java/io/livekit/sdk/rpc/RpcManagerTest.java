package io.livekit.sdk.rpc;

import static org.junit.jupiter.api.Assertions.*;

import io.livekit.sdk.RoomTransport;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import livekit.LivekitModels;
import livekit.LivekitRtc;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** Tests RPC request/response flow between two managers over a loopback transport. */
class RpcManagerTest {

  private RpcManager caller;
  private RpcManager callee;

  /** Transport that forwards packets to the other side's manager, simulating the SFU. */
  private class LoopbackTransport implements RoomTransport {
    private final String localIdentity;
    private final boolean toCallee;
    boolean dropPackets = false;
    boolean failSend = false;

    LoopbackTransport(String localIdentity, boolean toCallee) {
      this.localIdentity = localIdentity;
      this.toCallee = toCallee;
    }

    @Override
    public void sendUpdateMetadata(LivekitRtc.UpdateParticipantMetadata metadata) {}

    @Override
    public void sendUpdateSubscription(LivekitRtc.UpdateSubscription subscription) {}

    @Override
    public void sendUpdateTrackSettings(LivekitRtc.UpdateTrackSettings settings) {}

    @Override
    public void sendSubscriptionPermission(LivekitRtc.SubscriptionPermission permission) {}

    @Override
    public void sendMuteTrack(String trackSid, boolean muted) {}

    @Override
    public boolean sendDataPacket(LivekitModels.DataPacket packet, boolean reliable) {
      if (failSend) {
        return false;
      }
      if (dropPackets) {
        return true;
      }
      RpcManager target = toCallee ? callee : caller;
      if (target == null) {
        return true;
      }
      switch (packet.getValueCase()) {
        case RPC_REQUEST:
          target.handleRequest(localIdentity, packet.getRpcRequest());
          break;
        case RPC_ACK:
          target.handleAck(packet.getRpcAck());
          break;
        case RPC_RESPONSE:
          target.handleResponse(packet.getRpcResponse());
          break;
        default:
          break;
      }
      return true;
    }
  }

  private LoopbackTransport callerTransport;

  private void connect() {
    callerTransport = new LoopbackTransport("caller", true);
    caller = new RpcManager(callerTransport, 500);
    callee = new RpcManager(new LoopbackTransport("callee", false), 500);
  }

  @AfterEach
  void tearDown() {
    if (caller != null) {
      caller.close();
    }
    if (callee != null) {
      callee.close();
    }
  }

  @Test
  void performRpcReturnsHandlerResult() throws Exception {
    connect();
    callee.registerMethod(
        "greet", data -> "hello " + data.getCallerIdentity() + ":" + data.getPayload());

    String result = caller.performRpc("callee", "greet", "world").get(5, TimeUnit.SECONDS);
    assertEquals("hello caller:world", result);
  }

  @Test
  void unsupportedMethodReturnsError() {
    connect();
    CompletableFuture<String> future = caller.performRpc("callee", "missing", "");

    ExecutionException e =
        assertThrows(ExecutionException.class, () -> future.get(5, TimeUnit.SECONDS));
    assertInstanceOf(RpcError.class, e.getCause());
    assertEquals(RpcError.UNSUPPORTED_METHOD, ((RpcError) e.getCause()).getCode());
  }

  @Test
  void handlerRpcErrorIsPropagated() {
    connect();
    callee.registerMethod(
        "fail",
        data -> {
          throw new RpcError(2000, "custom failure", "extra");
        });

    ExecutionException e =
        assertThrows(
            ExecutionException.class,
            () -> caller.performRpc("callee", "fail", "").get(5, TimeUnit.SECONDS));
    RpcError error = (RpcError) e.getCause();
    assertEquals(2000, error.getCode());
    assertEquals("custom failure", error.getMessage());
    assertEquals("extra", error.getData());
  }

  @Test
  void handlerRuntimeExceptionBecomesApplicationError() {
    connect();
    callee.registerMethod(
        "boom",
        data -> {
          throw new RuntimeException("oops");
        });

    ExecutionException e =
        assertThrows(
            ExecutionException.class,
            () -> caller.performRpc("callee", "boom", "").get(5, TimeUnit.SECONDS));
    assertEquals(RpcError.APPLICATION_ERROR, ((RpcError) e.getCause()).getCode());
  }

  @Test
  void oversizedRequestPayloadFailsImmediately() {
    connect();
    String big = "x".repeat(RpcManager.MAX_PAYLOAD_BYTES + 1);

    ExecutionException e =
        assertThrows(
            ExecutionException.class,
            () -> caller.performRpc("callee", "greet", big).get(5, TimeUnit.SECONDS));
    assertEquals(RpcError.REQUEST_PAYLOAD_TOO_LARGE, ((RpcError) e.getCause()).getCode());
  }

  @Test
  void oversizedResponsePayloadReturnsError() {
    connect();
    callee.registerMethod("big", data -> "x".repeat(RpcManager.MAX_PAYLOAD_BYTES + 1));

    ExecutionException e =
        assertThrows(
            ExecutionException.class,
            () -> caller.performRpc("callee", "big", "").get(5, TimeUnit.SECONDS));
    assertEquals(RpcError.RESPONSE_PAYLOAD_TOO_LARGE, ((RpcError) e.getCause()).getCode());
  }

  @Test
  void noAckTimesOutWithConnectionTimeout() {
    connect();
    callerTransport.dropPackets = true;

    ExecutionException e =
        assertThrows(
            ExecutionException.class,
            () -> caller.performRpc("callee", "greet", "").get(5, TimeUnit.SECONDS));
    assertEquals(RpcError.CONNECTION_TIMEOUT, ((RpcError) e.getCause()).getCode());
  }

  @Test
  void sendFailureFailsImmediately() {
    connect();
    callerTransport.failSend = true;

    ExecutionException e =
        assertThrows(
            ExecutionException.class,
            () -> caller.performRpc("callee", "greet", "").get(5, TimeUnit.SECONDS));
    assertEquals(RpcError.SEND_FAILED, ((RpcError) e.getCause()).getCode());
  }

  @Test
  void participantDisconnectFailsPendingRequests() {
    connect();
    callerTransport.dropPackets = true;

    CompletableFuture<String> future = caller.performRpc("callee", "greet", "", 60000);
    caller.handleParticipantDisconnected("callee");

    ExecutionException e =
        assertThrows(ExecutionException.class, () -> future.get(5, TimeUnit.SECONDS));
    assertEquals(RpcError.RECIPIENT_DISCONNECTED, ((RpcError) e.getCause()).getCode());
  }

  @Test
  void duplicateMethodRegistrationThrows() {
    connect();
    callee.registerMethod("dup", data -> "a");
    assertThrows(IllegalArgumentException.class, () -> callee.registerMethod("dup", data -> "b"));
  }
}
