package com.legacyloop.career.controller;

import com.legacyloop.career.dto.CompanyDtos.CompanyRequest;
import com.legacyloop.career.dto.CompanyDtos.CompanyResponse;
import com.legacyloop.career.dto.JobDtos.JobDetail;
import com.legacyloop.career.dto.JobDtos.JobRequest;
import com.legacyloop.career.dto.JobDtos.JobSummary;
import com.legacyloop.career.service.JobService;
import com.legacyloop.common.ApiResponse;
import com.legacyloop.common.AuthUser;
import com.legacyloop.common.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Postings and the companies behind them. Only staff, alumni and admins may post. */
@Tag(name = "Jobs", description = "The job board, the staff console and employer records")
@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
public class JobController {

    private static final String CAN_POST = "hasAnyRole('INSTITUTION_STAFF','ALUMNI','PLATFORM_ADMIN')";

    private final JobService jobService;

    @GetMapping("/search")
    @Operation(summary = "The public board — open postings only")
    public ApiResponse<PageResponse<JobSummary>> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String jobType,
            @RequestParam(required = false) String workMode,
            @RequestParam(required = false) Long companyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        var pageable = PageRequest.of(page, Math.min(size, 50), Sort.by(Sort.Direction.DESC, "publishedAt"));
        return ApiResponse.ok(jobService.search(query, jobType, workMode, companyId, pageable));
    }

    @GetMapping("/manage")
    @PreAuthorize(CAN_POST)
    @Operation(summary = "Every posting including drafts — the staff console")
    public ApiResponse<PageResponse<JobSummary>> manage(
            @AuthenticationPrincipal AuthUser user,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long companyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long institutionId = user.isAdmin() ? null : user.institutionId();
        var pageable = PageRequest.of(page, Math.min(size, 50), Sort.by(Sort.Direction.DESC, "id"));
        return ApiResponse.ok(jobService.manage(query, status, companyId, institutionId, pageable));
    }

    @GetMapping("/mine")
    @PreAuthorize(CAN_POST)
    @Operation(summary = "Postings you created")
    public ApiResponse<PageResponse<JobSummary>> mine(@AuthenticationPrincipal AuthUser user,
                                                      @RequestParam(required = false) String status,
                                                      @RequestParam(defaultValue = "0") int page,
                                                      @RequestParam(defaultValue = "20") int size) {
        var pageable = PageRequest.of(page, Math.min(size, 50), Sort.by(Sort.Direction.DESC, "id"));
        return ApiResponse.ok(jobService.mine(user.id(), status, pageable));
    }

    @GetMapping("/companies")
    @Operation(summary = "Search employers")
    public ApiResponse<PageResponse<CompanyResponse>> companies(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Boolean verified,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(jobService.searchCompanies(query, verified, active,
                PageRequest.of(page, Math.min(size, 50), Sort.by("name"))));
    }

    @GetMapping("/companies/active")
    @Operation(summary = "Active employers, for the posting form's dropdown")
    public ApiResponse<List<CompanyResponse>> activeCompanies() {
        return ApiResponse.ok(jobService.activeCompanies());
    }

    @GetMapping("/companies/{companyId}")
    @Operation(summary = "One employer")
    public ApiResponse<CompanyResponse> company(@PathVariable Long companyId) {
        return ApiResponse.ok(jobService.company(companyId));
    }

    @PostMapping("/companies")
    @PreAuthorize(CAN_POST)
    @Operation(summary = "Add an employer")
    public ApiResponse<CompanyResponse> createCompany(@Valid @RequestBody CompanyRequest request) {
        return ApiResponse.ok(jobService.createCompany(request), "Company added");
    }

    @PutMapping("/companies/{companyId}")
    @PreAuthorize(CAN_POST)
    @Operation(summary = "Update an employer")
    public ApiResponse<CompanyResponse> updateCompany(@PathVariable Long companyId,
                                                      @Valid @RequestBody CompanyRequest request) {
        return ApiResponse.ok(jobService.updateCompany(companyId, request), "Company updated");
    }

    @PatchMapping("/companies/{companyId}/verify")
    @PreAuthorize("hasAnyRole('INSTITUTION_STAFF','PLATFORM_ADMIN')")
    @Operation(summary = "Mark an employer as verified by the placement cell")
    public ApiResponse<CompanyResponse> verifyCompany(@PathVariable Long companyId) {
        return ApiResponse.ok(jobService.setCompanyVerified(companyId, true), "Company verified");
    }

    @PatchMapping("/companies/{companyId}/deactivate")
    @PreAuthorize("hasAnyRole('INSTITUTION_STAFF','PLATFORM_ADMIN')")
    @Operation(summary = "Retire an employer")
    public ApiResponse<CompanyResponse> deactivateCompany(@PathVariable Long companyId) {
        return ApiResponse.ok(jobService.setCompanyActive(companyId, false), "Company deactivated");
    }

    @GetMapping("/{jobId}")
    @Operation(summary = "One posting, with your eligibility and existing application if you are a student")
    public ApiResponse<JobDetail> detail(@PathVariable Long jobId, @AuthenticationPrincipal AuthUser user) {
        return ApiResponse.ok(jobService.detail(jobId, user));
    }

    @PostMapping
    @PreAuthorize(CAN_POST)
    @Operation(summary = "Create a posting as a draft")
    public ApiResponse<JobDetail> create(@AuthenticationPrincipal AuthUser user,
                                         @Valid @RequestBody JobRequest request) {
        return ApiResponse.ok(jobService.create(request, user), "Job saved as a draft");
    }

    @PutMapping("/{jobId}")
    @PreAuthorize(CAN_POST)
    @Operation(summary = "Update a posting")
    public ApiResponse<JobDetail> update(@AuthenticationPrincipal AuthUser user, @PathVariable Long jobId,
                                         @Valid @RequestBody JobRequest request) {
        return ApiResponse.ok(jobService.update(jobId, request, user), "Job updated");
    }

    @PatchMapping("/{jobId}/publish")
    @PreAuthorize(CAN_POST)
    @Operation(summary = "Put a draft on the board")
    public ApiResponse<JobDetail> publish(@AuthenticationPrincipal AuthUser user, @PathVariable Long jobId) {
        return ApiResponse.ok(jobService.publish(jobId, user), "Job published");
    }

    @PatchMapping("/{jobId}/close")
    @PreAuthorize(CAN_POST)
    @Operation(summary = "Stop accepting applications")
    public ApiResponse<JobDetail> close(@AuthenticationPrincipal AuthUser user, @PathVariable Long jobId) {
        return ApiResponse.ok(jobService.close(jobId, user), "Job closed");
    }

    @PatchMapping("/{jobId}/reopen")
    @PreAuthorize(CAN_POST)
    @Operation(summary = "Accept applications again")
    public ApiResponse<JobDetail> reopen(@AuthenticationPrincipal AuthUser user, @PathVariable Long jobId) {
        return ApiResponse.ok(jobService.reopen(jobId, user), "Job reopened");
    }
}
