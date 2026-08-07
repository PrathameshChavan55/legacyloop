package com.legacyloop.career.controller;

import com.legacyloop.career.dto.AnalyticsDtos.DashboardResponse;
import com.legacyloop.career.service.AnalyticsService;
import com.legacyloop.common.ApiResponse;
import com.legacyloop.common.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Analytics", description = "The placement dashboard and CSV exports")
@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/dashboard")
    @Operation(summary = "The dashboard for whoever is asking — a student's funnel or the cohort's")
    public ApiResponse<DashboardResponse> dashboard(@AuthenticationPrincipal AuthUser user) {
        return ApiResponse.ok(analyticsService.dashboard(user));
    }

    @PostMapping("/dashboard/refresh")
    @Operation(summary = "Recompute the dashboard; the numbers are read live, so this simply returns them")
    public ApiResponse<DashboardResponse> refresh(@AuthenticationPrincipal AuthUser user) {
        return ApiResponse.ok(analyticsService.dashboard(user), "Dashboard refreshed");
    }

    /** Two reports, one endpoint: the report name is the path variable. */
    @GetMapping("/export/{report}")
    @PreAuthorize("hasAnyRole('INSTITUTION_STAFF','PLATFORM_ADMIN')")
    @Operation(summary = "Download 'applications' or 'placement-register' as CSV")
    public ResponseEntity<byte[]> export(@PathVariable String report) {
        String csv = switch (report) {
            case "applications" -> analyticsService.exportApplications();
            case "placement-register" -> analyticsService.exportPlacementRegister();
            default -> throw com.legacyloop.common.ApiException.notFound("Report", report);
        };
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"legacyloop-%s.csv\"".formatted(report))
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv.getBytes(StandardCharsets.UTF_8));
    }
}
