package com.legacyloop.auth.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Body of /internal/v1/users/bulk - one round trip for a whole page of user ids. */
public record BulkUserRequest(

        @NotEmpty(message = "At least one user id is required")
        @Size(max = 200, message = "At most 200 ids per call")
        List<Long> userIds) {
}
