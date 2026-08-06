package com.legacyloop.common;

import org.springframework.http.HttpStatus;

/** Stable machine-readable error codes. The HTTP status travels with the code. */
public enum ErrorCode {

    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "One or more fields are invalid"),
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "The request could not be processed"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Authentication is required"),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Incorrect email or password"),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "Your session has expired"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "You do not have permission to do that"),
    NOT_FOUND(HttpStatus.NOT_FOUND, "The requested resource was not found"),
    CONFLICT(HttpStatus.CONFLICT, "That conflicts with something that already exists"),
    PAYLOAD_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "The uploaded file is too large"),
    TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "Too many attempts, please try again later"),
    UPSTREAM_ERROR(HttpStatus.BAD_GATEWAY, "A dependent service is unavailable"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong on our side");

    private final HttpStatus status;
    private final String defaultMessage;

    ErrorCode(HttpStatus status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus status() {
        return status;
    }

    public String defaultMessage() {
        return defaultMessage;
    }
}
