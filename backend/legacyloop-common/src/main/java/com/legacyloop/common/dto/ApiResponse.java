package com.legacyloop.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.slf4j.MDC;

import java.time.Instant;

import static com.legacyloop.common.constant.SecurityConstants.MDC_CORRELATION_ID;

/**
 * Every endpoint in every service returns this envelope - success or failure.
 * The frontend switches on {@code error.code}, never on the message text.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        T data,
        String message,
        ErrorDetail error,
        Instant timestamp,
        String correlationId) {

    public static <T> ApiResponse<T> success(T data) {
        return success(data, null);
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(true, data, message, null, Instant.now(), MDC.get(MDC_CORRELATION_ID));
    }

    public static ApiResponse<Void> message(String message) {
        return new ApiResponse<>(true, null, message, null, Instant.now(), MDC.get(MDC_CORRELATION_ID));
    }

    public static <T> ApiResponse<T> failure(ErrorDetail error) {
        return new ApiResponse<>(false, null, error.message(), error, Instant.now(), MDC.get(MDC_CORRELATION_ID));
    }
}
