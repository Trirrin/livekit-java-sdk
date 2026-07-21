package io.livekit.sdk.rpc;

/** Handler for an RPC method registered on the local participant. */
@FunctionalInterface
public interface RpcHandler {

  /**
   * Handle an incoming RPC invocation.
   *
   * @return the response payload, at most 15KiB of UTF-8 text
   * @throws RpcError to return a structured error to the caller; any other exception is reported to
   *     the caller as a generic application error
   */
  String handle(RpcInvocationData data) throws Exception;
}
