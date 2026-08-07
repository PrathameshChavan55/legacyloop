package com.legacyloop.career.controller;

import com.legacyloop.career.dto.ApplicationDtos.ApplicationResponse;
import com.legacyloop.career.dto.ApplicationDtos.ApplicationReview;
import com.legacyloop.career.dto.ApplicationDtos.ApplyRequest;
import com.legacyloop.career.dto.ApplicationDtos.ReferralRequestBody;
import com.legacyloop.career.dto.ApplicationDtos.ReferralResponse;
import com.legacyloop.career.dto.ApplicationDtos.ReferralResponseBody;
import com.legacyloop.career.dto.ApplicationDtos.StatusChangeRequest;
import com.legacyloop.career.service.ApplicationService;
import com.legacyloop.common.ApiResponse;
import com.legacyloop.common.AuthUser;
import com.legacyloop.common.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Applying, reviewing and referrals — the three sides of one application. */
@Tag(name = "Applications", description = "Applying to jobs, the review pipeline and referrals")
@RestController
@RequestMapping("/api/v1/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;

    @PostMapping("/jobs/{jobId}")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Apply to a job")
    public ApiResponse<ApplicationResponse> apply(@AuthenticationPrincipal AuthUser user,
                                                  @PathVariable Long jobId,
                                                  @Valid @RequestBody ApplyRequest request) {
        return ApiResponse.ok(applicationService.apply(jobId, request, user), "Application submitted");
    }

    @GetMapping("/me")
    @Operation(summary = "Your applications")
    public ApiResponse<PageResponse<ApplicationResponse>> mine(@AuthenticationPrincipal AuthUser user,
                                                               @RequestParam(required = false) String status,
                                                               @RequestParam(defaultValue = "0") int page,
                                                               @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(applicationService.mine(user.id(), status,
                PageRequest.of(page, Math.min(size, 50))));
    }

    @GetMapping("/me/{applicationId}")
    @Operation(summary = "One of your applications")
    public ApiResponse<ApplicationResponse> mineById(@AuthenticationPrincipal AuthUser user,
                                                     @PathVariable Long applicationId) {
        return ApiResponse.ok(applicationService.mineById(applicationId, user.id()));
    }

    @PatchMapping("/me/{applicationId}/withdraw")
    @Operation(summary = "Withdraw an application")
    public ApiResponse<ApplicationResponse> withdraw(@AuthenticationPrincipal AuthUser user,
                                                     @PathVariable Long applicationId) {
        return ApiResponse.ok(applicationService.withdraw(applicationId, user.id()), "Application withdrawn");
    }

    @GetMapping("/referrals/received")
    @Operation(summary = "Referral requests sent to you")
    public ApiResponse<PageResponse<ReferralResponse>> referralsReceived(
            @AuthenticationPrincipal AuthUser user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(applicationService.referralsReceived(user.id(),
                PageRequest.of(page, Math.min(size, 50))));
    }

    @GetMapping("/referrals/sent")
    @Operation(summary = "Referral requests you sent")
    public ApiResponse<PageResponse<ReferralResponse>> referralsSent(
            @AuthenticationPrincipal AuthUser user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(applicationService.referralsSent(user.id(),
                PageRequest.of(page, Math.min(size, 50))));
    }

    @PatchMapping("/referrals/{referralId}/respond")
    @PreAuthorize("hasRole('ALUMNI')")
    @Operation(summary = "Accept or decline a referral request")
    public ApiResponse<ReferralResponse> respondToReferral(@AuthenticationPrincipal AuthUser user,
                                                           @PathVariable Long referralId,
                                                           @Valid @RequestBody ReferralResponseBody request) {
        return ApiResponse.ok(applicationService.respond(referralId, request, user), "Response recorded");
    }

    @PatchMapping("/referrals/{referralId}/withdraw")
    @Operation(summary = "Take back a referral request you sent")
    public ApiResponse<ReferralResponse> withdrawReferral(@AuthenticationPrincipal AuthUser user,
                                                          @PathVariable Long referralId) {
        return ApiResponse.ok(applicationService.withdrawReferral(referralId, user.id()), "Request withdrawn");
    }

    @GetMapping("/jobs/{jobId}")
    @PreAuthorize("hasAnyRole('INSTITUTION_STAFF','ALUMNI','PLATFORM_ADMIN')")
    @Operation(summary = "Applicants for one job")
    public ApiResponse<PageResponse<ApplicationResponse>> forJob(@AuthenticationPrincipal AuthUser user,
                                                                 @PathVariable Long jobId,
                                                                 @RequestParam(required = false) String status,
                                                                 @RequestParam(defaultValue = "0") int page,
                                                                 @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(applicationService.forJob(jobId, status, user,
                PageRequest.of(page, Math.min(size, 50))));
    }

    @GetMapping("/{applicationId}/review")
    @PreAuthorize("hasAnyRole('INSTITUTION_STAFF','ALUMNI','PLATFORM_ADMIN')")
    @Operation(summary = "The reviewer's view: notes, academic snapshot and the moves available")
    public ApiResponse<ApplicationReview> review(@AuthenticationPrincipal AuthUser user,
                                                 @PathVariable Long applicationId) {
        return ApiResponse.ok(applicationService.review(applicationId, user));
    }

    @PatchMapping("/{applicationId}/status")
    @PreAuthorize("hasAnyRole('INSTITUTION_STAFF','ALUMNI','PLATFORM_ADMIN')")
    @Operation(summary = "Move an application along the pipeline")
    public ApiResponse<ApplicationResponse> changeStatus(@AuthenticationPrincipal AuthUser user,
                                                         @PathVariable Long applicationId,
                                                         @Valid @RequestBody StatusChangeRequest request) {
        return ApiResponse.ok(applicationService.changeStatus(applicationId, request, user), "Status updated");
    }

    @GetMapping("/{applicationId}/referrers")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Alumni at this company who are open to referring")
    public ApiResponse<List<Map<String, Object>>> referrers(@AuthenticationPrincipal AuthUser user,
                                                            @PathVariable Long applicationId) {
        return ApiResponse.ok(applicationService.referrersFor(applicationId, user.id()));
    }

    @PostMapping("/{applicationId}/referrals")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Ask one or more alumni for a referral")
    public ApiResponse<List<ReferralResponse>> requestReferrals(@AuthenticationPrincipal AuthUser user,
                                                                @PathVariable Long applicationId,
                                                                @Valid @RequestBody ReferralRequestBody request) {
        return ApiResponse.ok(applicationService.requestReferrals(applicationId, request, user),
                "Referral requests sent");
    }
}
