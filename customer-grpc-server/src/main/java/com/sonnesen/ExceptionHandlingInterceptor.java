package com.sonnesen;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.grpc.ForwardingServerCallListener.SimpleForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;

/**
 * Converts exceptions thrown by service implementations into the matching
 * gRPC {@link Status}, in one place, instead of every RPC method having to
 * catch and translate them itself.
 *
 * <p>For unary calls, {@code onHalfClose()} is where the actual service
 * method runs (grpc-java invokes it once the single request message has
 * been fully received), so wrapping it here is enough to catch exceptions
 * thrown from anywhere in {@link CustomerServiceImpl}.
 */
public class ExceptionHandlingInterceptor implements ServerInterceptor {

    private static final Logger logger = LogManager.getLogger(ExceptionHandlingInterceptor.class);

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {

        ServerCall.Listener<ReqT> listener = next.startCall(call, headers);

        return new SimpleForwardingServerCallListener<>(listener) {
            @Override
            public void onHalfClose() {
                try {
                    super.onHalfClose();
                } catch (RuntimeException e) {
                    call.close(toStatus(e), new Metadata());
                }
            }
        };
    }

    private Status toStatus(RuntimeException e) {
        if (e instanceof CustomerNotFoundException) {
            return Status.NOT_FOUND.withDescription(e.getMessage());
        }
        if (e instanceof DuplicateEmailException) {
            return Status.ALREADY_EXISTS.withDescription(e.getMessage());
        }
        if (e instanceof IllegalArgumentException) {
            return Status.INVALID_ARGUMENT.withDescription(e.getMessage());
        }

        // Anything unexpected: log the full exception server-side, but don't
        // leak internals (stack trace, SQL, etc.) to the client.
        logger.error("Unhandled exception while processing gRPC call", e);
        return Status.INTERNAL.withDescription("Internal server error");
    }
}
