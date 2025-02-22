/**
 * Copyright (c) 2016-2022 Deephaven Data Labs and Patent Pending
 */
package io.deephaven.extensions.barrage.util;

import io.deephaven.io.logger.Logger;
import com.google.rpc.Code;
import io.deephaven.proto.util.Exceptions;
import io.deephaven.util.FunctionalInterfaces;
import io.deephaven.internal.log.LoggerFactory;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;

import java.util.UUID;

public class GrpcUtil {
    private static final Logger log = LoggerFactory.getLogger(GrpcUtil.class);

    public static StatusRuntimeException securelyWrapError(final Logger log, final Throwable err) {
        return securelyWrapError(log, err, Code.INVALID_ARGUMENT);
    }

    public static StatusRuntimeException securelyWrapError(final Logger log, final Throwable err,
            final Code statusCode) {
        if (err instanceof StatusRuntimeException) {
            return (StatusRuntimeException) err;
        }

        final UUID errorId = UUID.randomUUID();
        log.error().append("Internal Error '").append(errorId.toString()).append("' ").append(err).endl();
        return Exceptions.statusRuntimeException(statusCode, "Details Logged w/ID '" + errorId + "'");
    }

    /**
     * Wraps the provided runner in a try/catch block to minimize damage caused by a failing externally supplied helper.
     *
     * @param observer the stream that will be used in the runnable
     * @param runner the runnable to execute safely
     */
    private static void safelyExecuteLocked(final StreamObserver<?> observer,
            final FunctionalInterfaces.ThrowingRunnable<Exception> runner) {
        try {
            // noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized (observer) {
                runner.run();
            }
        } catch (final Exception err) {
            log.debug().append("Unanticipated gRPC Error: ").append(err).endl();
        }
    }

    /**
     * Sends one message to the stream, ignoring any errors that may happen during that call.
     *
     * @param observer the stream to complete
     * @param message the message to send on this stream
     * @param <T> the type of message that the stream handles
     */
    public static <T> void safelyOnNext(StreamObserver<T> observer, T message) {
        safelyExecuteLocked(observer, () -> observer.onNext(message));
    }

    /**
     * Sends one message and then completes the stream, ignoring any errors that may happen during these calls. Useful
     * for unary responses.
     *
     * @param observer the stream to complete
     * @param message the last message to send on this stream before completing
     * @param <T> the type of message that the stream handles
     */
    public static <T> void safelyComplete(StreamObserver<T> observer, T message) {
        safelyExecuteLocked(observer, () -> {
            observer.onNext(message);
            observer.onCompleted();
        });
    }

    /**
     * Completes the stream, ignoring any errors that may happen during this call.
     *
     * @param observer the stream to complete
     */
    public static void safelyComplete(StreamObserver<?> observer) {
        safelyExecuteLocked(observer, observer::onCompleted);
    }

    /**
     * Writes an error to the observer in a try/catch block to minimize damage caused by failing observer call.
     * <p>
     * </p>
     * This will always synchronize on the observer to ensure thread safety when interacting with the grpc response
     * stream.
     */
    public static void safelyError(final StreamObserver<?> observer, final Code statusCode, final String msg) {
        safelyError(observer, Exceptions.statusRuntimeException(statusCode, msg));
    }

    /**
     * Writes an error to the observer in a try/catch block to minimize damage caused by failing observer call.
     * <p>
     * </p>
     * This will always synchronize on the observer to ensure thread safety when interacting with the grpc response
     * stream.
     */
    public static void safelyError(final StreamObserver<?> observer, StatusRuntimeException exception) {
        safelyExecuteLocked(observer, () -> observer.onError(exception));
    }
}
