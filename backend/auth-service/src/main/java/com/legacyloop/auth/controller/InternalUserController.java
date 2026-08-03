package com.legacyloop.auth.controller;

import com.legacyloop.auth.dto.request.BulkUserRequest;
import com.legacyloop.auth.service.UserService;
import com.legacyloop.common.dto.ApiResponse;
import com.legacyloop.common.dto.UserSummaryDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Service-to-service only. The gateway blocks /internal/** from the public internet and
 * requires the shared internal API key, so there is no JWT on these calls.
 */
@RestController
@RequestMapping("/internal/v1/users")
@RequiredArgsConstructor
@Tag(name = "Internal", description = "Called by core, feed and payment services via OpenFeign")
public class InternalUserController {

    private final UserService userService;

    @GetMapping("/{userId}")
    @Operation(summary = "Narrow user summary for another service")
    public ResponseEntity<ApiResponse<UserSummaryDto>> summary(@PathVariable Long userId) {
        UserSummaryDto summary = userService.getSummary(userId);
        return ResponseEntity.ok(ApiResponse.success(
                summary == null ? UserSummaryDto.unknown(userId) : summary));
    }

    @PostMapping("/bulk")
    @Operation(summary = "Hydrate a page of users in one call",
            description = "Prevents the N+1 problem across the service boundary.")
    public ResponseEntity<ApiResponse<List<UserSummaryDto>>> bulk(@Valid @RequestBody BulkUserRequest request) {
        return ResponseEntity.ok(ApiResponse.success(userService.getSummaries(request.userIds())));
    }
}
