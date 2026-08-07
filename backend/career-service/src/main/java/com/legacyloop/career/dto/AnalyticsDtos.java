package com.legacyloop.career.dto;

import java.util.List;
import java.util.Map;

public final class AnalyticsDtos {

    private AnalyticsDtos() {
    }

    /**
     * The whole dashboard in one response.
     *
     * <p>Which sections are populated depends on who is asking: a student sees their own funnel,
     * staff see the cohort. The original returned four different shapes from four endpoints and
     * the frontend branched on the role anyway.
     */
    public record DashboardResponse(String audience,
                                    Map<String, Long> headline,
                                    List<CountByLabel> applicationsByStatus,
                                    List<CountByLabel> topCompanies,
                                    List<CountByLabel> applicationsOverTime,
                                    List<String> highlights) {
    }

    public record CountByLabel(String label, long count) {
    }
}
