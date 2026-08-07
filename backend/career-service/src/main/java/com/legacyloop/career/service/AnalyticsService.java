package com.legacyloop.career.service;

import com.legacyloop.career.dto.AnalyticsDtos.CountByLabel;
import com.legacyloop.career.dto.AnalyticsDtos.DashboardResponse;
import com.legacyloop.career.entity.Enums.ApplicationStatus;
import com.legacyloop.career.entity.Enums.JobStatus;
import com.legacyloop.career.entity.JobApplication;
import com.legacyloop.career.repository.JobApplicationRepository;
import com.legacyloop.career.repository.JobRepository;
import com.legacyloop.career.repository.ReferralRequestRepository;
import com.legacyloop.common.AuthUser;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The placement dashboard and the two CSV exports.
 *
 * <p>The original cached a materialised snapshot in a table and had a refresh endpoint to rebuild
 * it. At this data size the aggregate queries are fast enough to run on request, so the numbers
 * are never stale — the refresh endpoint is kept because the UI has a button for it, and it simply
 * recomputes.
 */
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final JobRepository jobs;
    private final JobApplicationRepository applications;
    private final ReferralRequestRepository referrals;
    private final UserClient userClient;

    @Transactional(readOnly = true)
    public DashboardResponse dashboard(AuthUser viewer) {
        return viewer.hasRole("STUDENT") ? studentDashboard(viewer) : staffDashboard();
    }

    /** A student sees their own funnel, not the cohort's. */
    private DashboardResponse studentDashboard(AuthUser student) {
        Map<String, Long> byStatus = toMap(applications.countGroupedByStatusForUser(student.id()));
        long total = byStatus.values().stream().mapToLong(Long::longValue).sum();
        long active = total - byStatus.getOrDefault(ApplicationStatus.REJECTED.name(), 0L)
                - byStatus.getOrDefault(ApplicationStatus.WITHDRAWN.name(), 0L);

        Map<String, Long> headline = new LinkedHashMap<>();
        headline.put("applications", total);
        headline.put("active", active);
        headline.put("interviews", byStatus.getOrDefault(ApplicationStatus.INTERVIEW_SCHEDULED.name(), 0L));
        headline.put("offers", byStatus.getOrDefault(ApplicationStatus.SELECTED.name(), 0L));
        headline.put("openJobs", jobs.countByStatus(JobStatus.OPEN));

        List<String> highlights = new ArrayList<>();
        if (total == 0) {
            highlights.add("You have not applied anywhere yet — the job board is a good place to start.");
        } else if (headline.get("offers") > 0) {
            highlights.add("You have an offer. Congratulations.");
        } else if (active == 0) {
            highlights.add("None of your applications are still live. Try a few more roles.");
        }

        return new DashboardResponse("STUDENT", headline, labelled(byStatus), List.of(),
                applicationsOverTime(applications.findMine(student.id(), null,
                        Pageable.ofSize(500)).getContent()), highlights);
    }

    private DashboardResponse staffDashboard() {
        Map<String, Long> byStatus = toMap(applications.countGroupedByStatus());
        long total = byStatus.values().stream().mapToLong(Long::longValue).sum();
        long selected = byStatus.getOrDefault(ApplicationStatus.SELECTED.name(), 0L);

        Map<String, Long> headline = new LinkedHashMap<>();
        headline.put("openJobs", jobs.countByStatus(JobStatus.OPEN));
        headline.put("draftJobs", jobs.countByStatus(JobStatus.DRAFT));
        headline.put("closedJobs", jobs.countByStatus(JobStatus.CLOSED));
        headline.put("applications", total);
        headline.put("offers", selected);
        headline.put("referralsAccepted", referrals.countByStatus(
                com.legacyloop.career.entity.Enums.ReferralStatus.ACCEPTED));
        headline.put("jobsThisMonth", jobs.countByPublishedAtAfter(
                Instant.now().minus(30, ChronoUnit.DAYS)));
        headline.putAll(userClient.placementStats());

        List<CountByLabel> topCompanies = jobs.countByCompany(Pageable.ofSize(5)).stream()
                .map(row -> new CountByLabel((String) row[0], ((Number) row[1]).longValue()))
                .toList();

        List<String> highlights = new ArrayList<>();
        if (total > 0) {
            highlights.add("%d%% of applications have ended in an offer.".formatted(selected * 100 / total));
        }
        if (headline.get("draftJobs") > 0) {
            highlights.add("%d postings are still drafts.".formatted(headline.get("draftJobs")));
        }

        return new DashboardResponse("STAFF", headline, labelled(byStatus), topCompanies,
                applicationsOverTime(applications.findAllByOrderByIdAsc()), highlights);
    }

    /* ---------------------------------------------------------------------------- exports */

    /** Every application as CSV, for the placement office's own records. */
    @Transactional(readOnly = true)
    public String exportApplications() {
        List<JobApplication> all = applications.findAllByOrderByIdAsc();
        Map<Long, UserClient.UserSummary> people = userClient.names(
                all.stream().map(JobApplication::getApplicantUserId).toList());

        StringBuilder csv = new StringBuilder(
                "Application ID,Applicant,Identifier,Job,Company,Status,Applied on,Offer\n");
        for (JobApplication application : all) {
            var person = people.get(application.getApplicantUserId());
            csv.append(row(
                    application.getId(),
                    person == null ? "Unknown" : person.fullName(),
                    person == null ? "" : person.studentIdentifier(),
                    application.getJob().getTitle(),
                    application.getJob().getCompany().getName(),
                    application.getStatus().label(),
                    application.getAppliedAt(),
                    application.getOfferedPackage()));
        }
        return csv.toString();
    }

    /** The placement register: who was selected, where, and for how much. */
    @Transactional(readOnly = true)
    public String exportPlacementRegister() {
        List<JobApplication> selected = applications.findByStatus(ApplicationStatus.SELECTED);
        Map<Long, UserClient.UserSummary> people = userClient.names(
                selected.stream().map(JobApplication::getApplicantUserId).toList());

        StringBuilder csv = new StringBuilder("Student,Identifier,Email,Company,Role,Package,Selected on\n");
        for (JobApplication application : selected) {
            var person = people.get(application.getApplicantUserId());
            csv.append(row(
                    person == null ? "Unknown" : person.fullName(),
                    person == null ? "" : person.studentIdentifier(),
                    person == null ? "" : person.email(),
                    application.getJob().getCompany().getName(),
                    application.getJob().getTitle(),
                    application.getOfferedPackage(),
                    application.getLastUpdatedAt()));
        }
        return csv.toString();
    }

    /* -------------------------------------------------------------------------- internals */

    /** Applications per day over the last fortnight, zero-filled so the chart has no gaps. */
    private List<CountByLabel> applicationsOverTime(List<JobApplication> source) {
        Map<LocalDate, Long> counts = new LinkedHashMap<>();
        LocalDate today = LocalDate.now();
        for (int daysAgo = 13; daysAgo >= 0; daysAgo--) {
            counts.put(today.minusDays(daysAgo), 0L);
        }
        for (JobApplication application : source) {
            if (application.getAppliedAt() == null) {
                continue;
            }
            LocalDate day = application.getAppliedAt().atZone(ZoneId.systemDefault()).toLocalDate();
            counts.computeIfPresent(day, (key, value) -> value + 1);
        }
        return counts.entrySet().stream()
                .map(entry -> new CountByLabel(entry.getKey().toString(), entry.getValue()))
                .toList();
    }

    private static Map<String, Long> toMap(List<Object[]> rows) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (Object[] row : rows) {
            counts.put(((Enum<?>) row[0]).name(), ((Number) row[1]).longValue());
        }
        return counts;
    }

    /** Turns UNDER_REVIEW into "Under review" for the chart legend. */
    private static List<CountByLabel> labelled(Map<String, Long> byStatus) {
        return byStatus.entrySet().stream()
                .map(entry -> new CountByLabel(ApplicationStatus.valueOf(entry.getKey()).label(),
                        entry.getValue()))
                .toList();
    }

    /** Quotes every cell, because a job title with a comma in it would otherwise shift the columns. */
    private static String row(Object... cells) {
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < cells.length; i++) {
            String value = cells[i] == null ? "" : cells[i].toString().replace("\"", "\"\"");
            line.append('"').append(value).append('"');
            line.append(i == cells.length - 1 ? "\n" : ",");
        }
        return line.toString();
    }
}
