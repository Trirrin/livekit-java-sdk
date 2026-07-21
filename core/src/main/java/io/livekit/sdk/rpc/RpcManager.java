package io.livekit.sdk.rpc;

import io.livekit.sdk.RoomTransport;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import livekit.LivekitModels;

/**
 * Implements the LiveKit RPC protocol (v1) over reliable data channels: outgoing invocations with
 * ack/response tracking, and dispatch of incoming invocations to registered handlers.
 */
public class RpcManager {

  /** Maximum payload size for requests and responses (15KiB). */
  public static final int MAX_PAYLOAD_BYTES = 15360;

  private static final long DEFAULT_RESPONSE_TIMEOUT_MS = 10000;
  private static final long CONNECTION_TIMEOUT_MS = 2000;
  private static final int RPC_VERSION = 1;

  private final RoomTransport transport;
  private final Map<String, RpcHandler> handlers = new ConcurrentHashMap<>();
  private final Map<String, PendingRpc> pending = new ConcurrentHashMap<>();
  private final ScheduledExecutorService scheduler;
  private final ExecutorService handlerExecutor;

  private static final class PendingRpc {
    final String destinationIdentity;
    final CompletableFuture<String> future;
    volatile boolean acked;
    volatile ScheduledFuture<?> ackTimeout;
    volatile ScheduledFuture<?> responseTimeout;

    PendingRpc(String destinationIdentity, CompletableFuture<String> future) {
      this.destinationIdentity = destinationIdentity;
      this.future = future;
    }

    void cancelTimers() {
      if (ackTimeout != null) {
        ackTimeout.cancel(false);
      }
      if (responseTimeout != null) {
        responseTimeout.cancel(false);
      }
    }
  }

  private final long connectionTimeoutMs;

  public RpcManager(RoomTransport transport) {
    this(transport, CONNECTION_TIMEOUT_MS);
  }

  RpcManager(RoomTransport transport, long connectionTimeoutMs) {
    this.transport = transport;
    this.connectionTimeoutMs = connectionTimeoutMs;
    this.scheduler =
        Executors.newSingleThreadScheduledExecutor(
            runnable -> {
              Thread thread = new Thread(runnable, "lk-rpc-timer");
              thread.setDaemon(true);
              return thread;
            });
    this.handlerExecutor =
        Executors.newCachedThreadPool(
            runnable -> {
              Thread thread = new Thread(runnable, "lk-rpc-handler");
              thread.setDaemon(true);
              return thread;
            });
  }

  /** Register a handler for an RPC method. Throws if the method is already registered. */
  public void registerMethod(String method, RpcHandler handler) {
    if (handlers.putIfAbsent(method, handler) != null) {
      throw new IllegalArgumentException("RPC method already registered: " + method);
    }
  }

  public void unregisterMethod(String method) {
    handlers.remove(method);
  }

  /** Invoke an RPC method on a remote participant with the default 10s response timeout. */
  public CompletableFuture<String> performRpc(
      String destinationIdentity, String method, String payload) {
    return performRpc(destinationIdentity, method, payload, DEFAULT_RESPONSE_TIMEOUT_MS);
  }

  /**
   * Invoke an RPC method on a remote participant.
   *
   * @param destinationIdentity identity of the participant to invoke the method on
   * @param method method name
   * @param payload request payload, at most 15KiB of UTF-8 text
   * @param responseTimeoutMs total time to wait for the response
   */
  public CompletableFuture<String> performRpc(
      String destinationIdentity, String method, String payload, long responseTimeoutMs) {
    CompletableFuture<String> future = new CompletableFuture<>();

    if (byteLength(payload) > MAX_PAYLOAD_BYTES) {
      future.completeExceptionally(RpcError.builtIn(RpcError.REQUEST_PAYLOAD_TOO_LARGE));
      return future;
    }

    String requestId = UUID.randomUUID().toString();
    PendingRpc entry = new PendingRpc(destinationIdentity, future);
    pending.put(requestId, entry);

    long effectiveTimeout = Math.max(responseTimeoutMs - connectionTimeoutMs, 1000);
    LivekitModels.DataPacket packet =
        LivekitModels.DataPacket.newBuilder()
            .addDestinationIdentities(destinationIdentity)
            .setRpcRequest(
                LivekitModels.RpcRequest.newBuilder()
                    .setId(requestId)
                    .setMethod(method)
                    .setPayload(payload == null ? "" : payload)
                    .setResponseTimeoutMs((int) effectiveTimeout)
                    .setVersion(RPC_VERSION))
            .build();

    if (!transport.sendDataPacket(packet, true)) {
      pending.remove(requestId);
      future.completeExceptionally(RpcError.builtIn(RpcError.SEND_FAILED));
      return future;
    }

    entry.ackTimeout =
        scheduler.schedule(
            () -> {
              if (!entry.acked) {
                failPending(requestId, RpcError.builtIn(RpcError.CONNECTION_TIMEOUT));
              }
            },
            connectionTimeoutMs,
            TimeUnit.MILLISECONDS);
    entry.responseTimeout =
        scheduler.schedule(
            () -> failPending(requestId, RpcError.builtIn(RpcError.RESPONSE_TIMEOUT)),
            responseTimeoutMs,
            TimeUnit.MILLISECONDS);

    return future;
  }

  /** Handle an incoming RPC request destined for the local participant. */
  public void handleRequest(String callerIdentity, LivekitModels.RpcRequest request) {
    sendAck(callerIdentity, request.getId());

    if (request.getVersion() != RPC_VERSION) {
      sendErrorResponse(
          callerIdentity, request.getId(), RpcError.builtIn(RpcError.UNSUPPORTED_VERSION));
      return;
    }

    RpcHandler handler = handlers.get(request.getMethod());
    if (handler == null) {
      sendErrorResponse(
          callerIdentity, request.getId(), RpcError.builtIn(RpcError.UNSUPPORTED_METHOD));
      return;
    }

    RpcInvocationData data =
        new RpcInvocationData(
            request.getId(), callerIdentity, request.getPayload(), request.getResponseTimeoutMs());
    handlerExecutor.execute(
        () -> {
          try {
            String response = handler.handle(data);
            if (byteLength(response) > MAX_PAYLOAD_BYTES) {
              sendErrorResponse(
                  callerIdentity,
                  request.getId(),
                  RpcError.builtIn(RpcError.RESPONSE_PAYLOAD_TOO_LARGE));
              return;
            }
            sendPayloadResponse(callerIdentity, request.getId(), response == null ? "" : response);
          } catch (RpcError error) {
            sendErrorResponse(callerIdentity, request.getId(), error);
          } catch (Exception e) {
            sendErrorResponse(
                callerIdentity, request.getId(), RpcError.builtIn(RpcError.APPLICATION_ERROR));
          }
        });
  }

  /** Handle an incoming ack for an outgoing request. */
  public void handleAck(LivekitModels.RpcAck ack) {
    PendingRpc entry = pending.get(ack.getRequestId());
    if (entry != null) {
      entry.acked = true;
      if (entry.ackTimeout != null) {
        entry.ackTimeout.cancel(false);
      }
    }
  }

  /** Handle an incoming response for an outgoing request. */
  public void handleResponse(LivekitModels.RpcResponse response) {
    PendingRpc entry = pending.remove(response.getRequestId());
    if (entry == null) {
      return;
    }
    entry.cancelTimers();
    if (response.hasError()) {
      entry.future.completeExceptionally(RpcError.fromProto(response.getError()));
    } else {
      entry.future.complete(response.getPayload());
    }
  }

  /** Fail outstanding requests to a participant that disconnected. */
  public void handleParticipantDisconnected(String identity) {
    for (Map.Entry<String, PendingRpc> mapEntry : pending.entrySet()) {
      if (mapEntry.getValue().destinationIdentity.equals(identity)) {
        failPending(mapEntry.getKey(), RpcError.builtIn(RpcError.RECIPIENT_DISCONNECTED));
      }
    }
  }

  /** Fail all outstanding requests, e.g. when the room disconnects. */
  public void failAllPending() {
    for (String requestId : pending.keySet()) {
      failPending(requestId, RpcError.builtIn(RpcError.RECIPIENT_DISCONNECTED));
    }
  }

  /** Fail all outstanding requests and stop background threads. */
  public void close() {
    failAllPending();
    scheduler.shutdownNow();
    handlerExecutor.shutdownNow();
  }

  private void failPending(String requestId, RpcError error) {
    PendingRpc entry = pending.remove(requestId);
    if (entry != null) {
      entry.cancelTimers();
      entry.future.completeExceptionally(error);
    }
  }

  private void sendAck(String destinationIdentity, String requestId) {
    LivekitModels.DataPacket packet =
        LivekitModels.DataPacket.newBuilder()
            .addDestinationIdentities(destinationIdentity)
            .setRpcAck(LivekitModels.RpcAck.newBuilder().setRequestId(requestId))
            .build();
    transport.sendDataPacket(packet, true);
  }

  private void sendPayloadResponse(String destinationIdentity, String requestId, String payload) {
    LivekitModels.DataPacket packet =
        LivekitModels.DataPacket.newBuilder()
            .addDestinationIdentities(destinationIdentity)
            .setRpcResponse(
                LivekitModels.RpcResponse.newBuilder().setRequestId(requestId).setPayload(payload))
            .build();
    transport.sendDataPacket(packet, true);
  }

  private void sendErrorResponse(String destinationIdentity, String requestId, RpcError error) {
    LivekitModels.DataPacket packet =
        LivekitModels.DataPacket.newBuilder()
            .addDestinationIdentities(destinationIdentity)
            .setRpcResponse(
                LivekitModels.RpcResponse.newBuilder()
                    .setRequestId(requestId)
                    .setError(error.toProto()))
            .build();
    transport.sendDataPacket(packet, true);
  }

  private static int byteLength(String value) {
    return value == null ? 0 : value.getBytes(StandardCharsets.UTF_8).length;
  }
}
