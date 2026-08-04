package com.legacyloop.core.dto.response;

import java.util.Map;

/** Feeds the placement-head dashboard charts (SRS REQ-5.2). */
public record PlacementStatsResponse(
        long totalJobs,
        long openJobs,
        long totalApplications,
        long selectedCount,
        long referralCount,
        double placementRate,
        Double averageAtsScore,
        Map<String, Long> applicationsByStatus) {
}
