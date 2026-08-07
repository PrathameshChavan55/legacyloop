package com.legacyloop.career.dto;

import com.legacyloop.career.entity.JobApplication;
import com.legacyloop.career.entity.ReferralRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;

public final class ApplicationDtos {

    private ApplicationDtos() {
    }

    public record ApplyRequest(Long resumeId, @Size(max = 5000) String coverLetter) {
    }

    public record StatusChangeRequest(@NotBlank String status,
                                      @Size(max = 500) String message,
                                      @Size(max = 2000) String reviewerNotes,
                                      Instant interviewAt,
                                      @Size(max = 255) String interviewLocation,
                                      BigDecimal offeredPackage) {
    }

    public record ReferralRequestBody(@Size(max = 2000) String message, java.util.List<Long> referrerUserIds) {
    }

    public record ReferralResponseBody(@NotBlank String decision, @Size(max = 2000) String note) {
    }

    /** What the applicant sees about their own application. */
    public record ApplicationResponse(Long id, Long jobId, String jobTitle, String companyName,
                                      String companyLogoUrl, String location, Long applicantUserId,
                                      String applicantName, String applicantEmail, String status,
                                      String statusLabel, int progress, boolean terminal, Long resumeId,
                                      String coverLetter, String statusMessage, Instant interviewAt,
                                      String interviewLocation, BigDecimal offeredPackage, int referralCount,
                                      Instant appliedAt, Instant lastUpdatedAt) {

        public static ApplicationResponse from(JobApplication application, String applicantName,
                                               String applicantEmail) {
            return new ApplicationResponse(application.getId(), application.getJob().getId(),
                    application.getJob().getTitle(), application.getJob().getCompany().getName(),
                    application.getJob().getCompany().getLogoUrl(), application.getJob().getLocation(),
                    application.getApplicantUserId(), applicantName, applicantEmail,
                    application.getStatus().name(), application.getStatus().label(),
                    application.getStatus().progress(), application.getStatus().isTerminal(),
                    application.getResumeId(), application.getCoverLetter(), application.getStatusMessage(),
                    application.getInterviewAt(), application.getInterviewLocation(),
                    application.getOfferedPackage(), application.getReferralCount(),
                    application.getAppliedAt(), application.getLastUpdatedAt());
        }
    }

    /**
     * The reviewer's view: the applicant's academic snapshot and the private notes, neither of
     * which belongs in {@link ApplicationResponse}.
     */
    public record ApplicationReview(ApplicationResponse application, String reviewerNotes,
                                    java.util.Map<String, Object> applicantSnapshot,
                                    java.util.List<String> allowedNextStatuses) {
    }

    public record ReferralResponse(Long id, Long applicationId, Long jobId, String jobTitle,
                                   String companyName, Long requesterUserId, String requesterName,
                                   Long referrerUserId, String referrerName, String message, String note,
                                   String status, Instant requestedAt, Instant respondedAt) {

        public static ReferralResponse from(ReferralRequest referral, String requesterName,
                                            String referrerName) {
            JobApplication application = referral.getApplication();
            return new ReferralResponse(referral.getId(), application.getId(), application.getJob().getId(),
                    application.getJob().getTitle(), application.getJob().getCompany().getName(),
                    referral.getRequesterUserId(), requesterName, referral.getReferrerUserId(), referrerName,
                    referral.getMessage(), referral.getNote(), referral.getStatus().name(),
                    referral.getRequestedAt(), referral.getRespondedAt());
        }
    }
}

