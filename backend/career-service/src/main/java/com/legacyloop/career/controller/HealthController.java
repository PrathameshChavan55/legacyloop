package com.legacyloop.career.controller;

import com.legacyloop.common.ApiResponse;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Proof that the service starts and that the shared response envelope is wired up.
 *
 * <p>{@code GET http://localhost:8082/internal/health} — no token needed. Keep this
 * endpoint; it is what docker-compose and the demo script check.
 */
@RestController
@RequestMapping("/internal/health")
public class HealthController {

    @GetMapping
    public ApiResponse<Map<String, String>> health() {
        return ApiResponse.ok(Map.of("service", "career-service", "status", "UP"));
    }
}
