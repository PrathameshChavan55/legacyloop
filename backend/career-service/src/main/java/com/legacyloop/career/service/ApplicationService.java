package com.legacyloop.career.service;

import com.legacyloop.career.dto.ApplicationDtos.ApplicationResponse;
import com.legacyloop.career.dto.ApplicationDtos.ApplicationReview;
import com.legacyloop.career.dto.ApplicationDtos.ApplyRequest;
import com.legacyloop.career.dto.ApplicationDtos.ReferralRequestBody;
import com.legacyloop.career.dto.ApplicationDtos.ReferralResponse;
import com.legacyloop.career.dto.ApplicationDtos.ReferralResponseBody;
import com.legacyloop.career.dto.ApplicationDtos.StatusChangeRequest;
import com.legacyloop.career.entity.Enums.ApplicationStatus;
import com.legacyloop.career.entity.Enums.ReferralStatus;
import com.legacyloop.career.entity.Job;
import com.legacyloop.career.entity.JobApplication;
import com.legacyloop.career.entity.ReferralRequest;
import com.legacyloop.career.repository.JobApplicationRepository;
import com.legacyloop.career.repository.ReferralRequestRepository;
import com.legacyloop.career.repository.ResumeRepository;
import com.legacyloop.common.ApiException;
import com.legacyloop.common.AuthUser;
import com.legacyloop.common.EventPublisher;
import com.legacyloop.common.Events;
import com.legacyloop.common.PageResponse;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Applying to jobs, moving applications through the pipeline, and referrals.
 *
 * <p>Referrals live here rather than in their own service because a referral only ever exists
 * against an application: splitting them meant every referral screen loaded the application from
 * a second service to render a job title.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final JobApplicationRepository applications;
    private final ReferralRequestRepository referrals;
    private final ResumeRepository resumes;
    private final JobService jobService;
    private final UserClient userClient;
    private final EventPublisher events;

    /* -------------------------------------------------------------------------- applying */

    @Transactional
    public ApplicationResponse apply(Long jobId, ApplyRequest request, AuthUser student) {
        Job job = jobService.load(jobId);

        if (!job.isAcceptingApplications()) {
            throw ApiException.badRequest("This job is no longer accepting applications");
        }
        if (applications.existsByJobIdAndApplicantUserId(jobId, student.id())) {
            throw ApiException.conflict("You have already applied to this job");
        }
        var eligibility = jobService.checkEligibility(job, student.id());
        if (!eligibility.eligible()) {
            throw ApiException.badRequest(eligibility.reasons().isEmpty()
                    ? "You are not eligible for this role"
                    : String.join(" ", eligibility.reasons()));
        }

        Long resumeId = request.resumeId() != null ? request.resumeId()
                : resumes.findFirstByOwnerUserIdAndPrimaryTrue(student.id())
                        .map(resume -> resume.getId()).orElse(null);
        if (resumeId == null) {
            throw ApiException.badRequest("Upload a resume before applying");
        }
        resumes.findByIdAndOwnerUserId(resumeId, student.id())
                .orElseThrow(() -> ApiException.badRequest("That resume does not belong to you"));

        JobApplication application = applications.save(JobApplication.builder()
                .job(job)
                .applicantUserId(student.id())
                .resumeId(resumeId)
                .coverLetter(request.coverLetter())
                .build());

        job.setApplicationCount(job.getApplicationCount() + 1);

        events.publish(Events.APPLICATION_SUBMITTED, job.getPostedByUserId(), "New application",
                "%s applied for %s.".formatted(student.fullName(), job.getTitle()),
                "/jobs/" + job.getId() + "/applicants");
        log.info("User {} applied to job {}", student.id(), jobId);

        return ApplicationResponse.from(application, student.fullName(), student.email());
    }

    @Transactional(readOnly = true)
    public PageResponse<ApplicationResponse> mine(Long userId, String status, Pageable pageable) {
        Page<JobApplication> page = applications.findMine(userId, status(status), pageable);
        return PageResponse.of(page, application -> ApplicationResponse.from(application, null, null));
    }

    @Transactional(readOnly = true)
    public ApplicationResponse mineById(Long applicationId, Long userId) {
        JobApplication application = applications.findByIdAndApplicantUserId(applicationId, userId)
                .orElseThrow(() -> ApiException.notFound("Application", applicationId));
        return ApplicationResponse.from(application, null, null);
    }

    @Transactional
    public ApplicationResponse withdraw(Long applicationId, Long userId) {
        JobApplication application = applications.findByIdAndApplicantUserId(applicationId, userId)
                .orElseThrow(() -> ApiException.notFound("Application", applicationId));
        if (application.getStatus().isTerminal()) {
            throw ApiException.badRequest("This application can no longer be withdrawn");
        }
        application.setStatus(ApplicationStatus.WITHDRAWN);
        return ApplicationResponse.from(application, null, null);
    }

    /* ------------------------------------------------------------------------ reviewing */

    /** The applicant list for one job, with names resolved in a single call to user-service. */
    @Transactional(readOnly = true)
    public PageResponse<ApplicationResponse> forJob(Long jobId, String status, AuthUser reviewer,
                                                    Pageable pageable) {
        Job job = jobService.load(jobId);
        requireReviewer(job, reviewer);

        Page<JobApplication> page = applications.findForJob(jobId, status(status), pageable);
        Map<Long, UserClient.UserSummary> people = userClient.names(
                page.getContent().stream().map(JobApplication::getApplicantUserId).toList());

        return PageResponse.of(page, application -> {
            var person = people.get(application.getApplicantUserId());
            return ApplicationResponse.from(application,
                    person == null ? "Unknown user" : person.fullName(),
                    person == null ? null : person.email());
        });
    }

    @Transactional(readOnly = true)
    public ApplicationReview review(Long applicationId, AuthUser reviewer) {
        JobApplication application = load(applicationId);
        requireReviewer(application.getJob(), reviewer);

        var person = userClient.user(application.getApplicantUserId());
        Map<String, Object> snapshot = userClient.eligibilitySnapshot(application.getApplicantUserId());

        return new ApplicationReview(
                ApplicationResponse.from(application, person.fullName(), person.email()),
                application.getReviewerNotes(),
                snapshot,
                application.getStatus().allowedNext().stream().map(Enum::name).toList());
    }

    /**
     * Moves an application along the pipeline.
     *
     * <p>The transition rules live on {@link ApplicationStatus}, so an invalid move is rejected in
     * one line here. Recording a selection also tells user-service to mark the student placed.
     */
    @Transactional
    public ApplicationResponse changeStatus(Long applicationId, StatusChangeRequest request,
                                            AuthUser reviewer) {
        JobApplication application = load(applicationId);
        requireReviewer(application.getJob(), reviewer);

        ApplicationStatus target = status(request.status());
        if (target == null) {
            throw ApiException.badRequest("Choose a status");
        }
        if (!application.getStatus().canMoveTo(target)) {
            throw ApiException.badRequest("An application cannot move from %s to %s"
                    .formatted(application.getStatus().label(), target.label()));
        }

        application.setStatus(target);
        application.setStatusMessage(request.message());
        if (request.reviewerNotes() != null) {
            application.setReviewerNotes(request.reviewerNotes());
        }
        if (target == ApplicationStatus.INTERVIEW_SCHEDULED) {
            application.setInterviewAt(request.interviewAt());
            application.setInterviewLocation(request.interviewLocation());
        }
        if (target == ApplicationStatus.SELECTED) {
            application.setOfferedPackage(request.offeredPackage());
            userClient.markPlaced(application.getApplicantUserId(),
                    application.getJob().getCompany().getName(), request.offeredPackage());
        }

        events.publish(Events.APPLICATION_STATUS_CHANGED, application.getApplicantUserId(),
                "Application update",
                "Your application for %s is now %s.".formatted(application.getJob().getTitle(), target.label()),
                "/applications/" + application.getId());

        var person = userClient.user(application.getApplicantUserId());
        return ApplicationResponse.from(application, person.fullName(), person.email());
    }

    /* ------------------------------------------------------------------------ referrals */

    /** Alumni at this job's company who are willing to refer. */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> referrersFor(Long applicationId, Long userId) {
        JobApplication application = applications.findByIdAndApplicantUserId(applicationId, userId)
                .orElseThrow(() -> ApiException.notFound("Application", applicationId));
        return userClient.referrersAt(application.getJob().getCompany().getName());
    }

    @Transactional
    public List<ReferralResponse> requestReferrals(Long applicationId, ReferralRequestBody request,
                                                   AuthUser student) {
        JobApplication application = applications.findByIdAndApplicantUserId(applicationId, student.id())
                .orElseThrow(() -> ApiException.notFound("Application", applicationId));

        if (!application.getJob().isReferralsEnabled()) {
            throw ApiException.badRequest("Referrals are not enabled for this job");
        }
        if (request.referrerUserIds() == null || request.referrerUserIds().isEmpty()) {
            throw ApiException.badRequest("Choose at least one alumnus to ask");
        }

        List<ReferralResponse> created = request.referrerUserIds().stream().distinct()
                .filter(referrerId -> !referrals.existsByApplicationIdAndReferrerUserId(applicationId, referrerId))
                .map(referrerId -> {
                    ReferralRequest referral = referrals.save(ReferralRequest.builder()
                            .application(application)
                            .requesterUserId(student.id())
                            .referrerUserId(referrerId)
                            .message(request.message())
                            .build());

                    events.publish(Events.REFERRAL_REQUESTED, referrerId, "Referral request",
                            "%s asked you for a referral at %s.".formatted(student.fullName(),
                                    application.getJob().getCompany().getName()),
                            "/referrals");

                    return ReferralResponse.from(referral, student.fullName(), null);
                })
                .toList();

        application.setReferralCount(application.getReferralCount() + created.size());
        return created;
    }

    @Transactional(readOnly = true)
    public PageResponse<ReferralResponse> referralsReceived(Long userId, Pageable pageable) {
        Page<ReferralRequest> page = referrals.findByReferrerUserIdOrderByIdDesc(userId, pageable);
        Map<Long, UserClient.UserSummary> people = userClient.names(
                page.getContent().stream().map(ReferralRequest::getRequesterUserId).toList());
        return PageResponse.of(page, referral -> ReferralResponse.from(referral,
                nameOf(people, referral.getRequesterUserId()), null));
    }

    @Transactional(readOnly = true)
    public PageResponse<ReferralResponse> referralsSent(Long userId, Pageable pageable) {
        Page<ReferralRequest> page = referrals.findByRequesterUserIdOrderByIdDesc(userId, pageable);
        Map<Long, UserClient.UserSummary> people = userClient.names(
                page.getContent().stream().map(ReferralRequest::getReferrerUserId).toList());
        return PageResponse.of(page, referral -> ReferralResponse.from(referral, null,
                nameOf(people, referral.getReferrerUserId())));
    }

    /**
     * The alumnus accepts or declines.
     *
     * <p>Accepting also advances the application to REFERRED when the pipeline allows it, so the
     * student sees the effect without the alumnus having to do anything else.
     */
    @Transactional
    public ReferralResponse respond(Long referralId, ReferralResponseBody request, AuthUser referrer) {
        ReferralRequest referral = referrals.findById(referralId)
                .orElseThrow(() -> ApiException.notFound("Referral", referralId));

        if (!referral.getReferrerUserId().equals(referrer.id())) {
            throw ApiException.forbidden("That referral request was not sent to you");
        }
        if (referral.getStatus() != ReferralStatus.REQUESTED) {
            throw ApiException.badRequest("You have already responded to this request");
        }

        boolean accepted = "ACCEPT".equalsIgnoreCase(request.decision())
                || "ACCEPTED".equalsIgnoreCase(request.decision());
        referral.setStatus(accepted ? ReferralStatus.ACCEPTED : ReferralStatus.DECLINED);
        referral.setNote(request.note());
        referral.setRespondedAt(Instant.now());

        if (accepted) {
            JobApplication application = referral.getApplication();
            if (application.getStatus().canMoveTo(ApplicationStatus.REFERRED)) {
                application.setStatus(ApplicationStatus.REFERRED);
            }
        }

        events.publish(Events.APPLICATION_STATUS_CHANGED, referral.getRequesterUserId(),
                accepted ? "Referral accepted" : "Referral declined",
                "%s %s your referral request.".formatted(referrer.fullName(),
                        accepted ? "accepted" : "could not take"),
                "/referrals");

        return ReferralResponse.from(referral, null, referrer.fullName());
    }

    @Transactional
    public ReferralResponse withdrawReferral(Long referralId, Long userId) {
        ReferralRequest referral = referrals.findById(referralId)
                .orElseThrow(() -> ApiException.notFound("Referral", referralId));
        if (!referral.getRequesterUserId().equals(userId)) {
            throw ApiException.forbidden("You can only withdraw a request you sent");
        }
        if (referral.getStatus() != ReferralStatus.REQUESTED) {
            throw ApiException.badRequest("That request has already been answered");
        }
        referral.setStatus(ReferralStatus.CANCELLED);
        referral.setRespondedAt(Instant.now());
        return ReferralResponse.from(referral, null, null);
    }

    private JobApplication load(Long applicationId) {
        return applications.findById(applicationId)
                .orElseThrow(() -> ApiException.notFound("Application", applicationId));
    }

    /** Whoever posted the job, staff or alumni at the same institution, or an admin. */
    private void requireReviewer(Job job, AuthUser reviewer) {
        if (reviewer.isAdmin() || job.getPostedByUserId().equals(reviewer.id())) {
            return;
        }
        boolean sameInstitution = (reviewer.isStaff() || reviewer.hasRole("ALUMNI"))
                && (job.getInstitutionId() == null || reviewer.institutionId() == null
                || job.getInstitutionId().equals(reviewer.institutionId()));
        if (!sameInstitution) {
            throw ApiException.forbidden("You cannot review applications for this job");
        }
    }

    private static String nameOf(Map<Long, UserClient.UserSummary> people, Long userId) {
        var person = people.get(userId);
        return person == null ? "Unknown user" : person.fullName();
    }

    private static ApplicationStatus status(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return ApplicationStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw ApiException.badRequest("Unknown application status: " + value);
        }
    }
}
