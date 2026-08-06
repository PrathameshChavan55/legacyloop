package com.legacyloop.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;

/** The envelope every endpoint returns: {success, message, data, error, timestamp}. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(boolean success, String message, T data, Error error, Instant timestamp) {

    public record Error(String code, String message, List<FieldError> fieldErrors) {}

    public record FieldError(String field, String message) {}

    public static <T> ApiResponse<T> ok(T data) {
        return ok(data, "Request completed successfully");
    }

    public static <T> ApiResponse<T> ok(T data, String message) {
        return new ApiResponse<>(true, message, data, null, Instant.now());
    }

    public static ApiResponse<Void> message(String message) {
        return new ApiResponse<>(true, message, null, null, Instant.now());
    }

    public static <T> ApiResponse<T> failed(String code, String message, List<FieldError> fieldErrors) {
        return new ApiResponse<>(false, message, null, new Error(code, message, fieldErrors), Instant.now());
    }
}
