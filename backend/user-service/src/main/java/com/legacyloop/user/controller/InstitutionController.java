package com.legacyloop.user.controller;

import com.legacyloop.common.ApiResponse;
import com.legacyloop.common.PageResponse;
import com.legacyloop.user.dto.InstitutionDtos.BrandingResponse;
import com.legacyloop.user.dto.InstitutionDtos.InstitutionRequest;
import com.legacyloop.user.dto.InstitutionDtos.InstitutionResponse;
import com.legacyloop.user.service.InstitutionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Institutions", description = "Tenants, their branding and their identifier rules")
@RestController
@RequestMapping("/api/v1/institutions")
@RequiredArgsConstructor
public class InstitutionController {

    private final InstitutionService institutionService;

    @GetMapping("/branding")
    @Operation(summary = "Public branding for every active institution — used by the sign-up page")
    public ApiResponse<List<BrandingResponse>> listBranding() {
        return ApiResponse.ok(institutionService.listBranding());
    }

    @GetMapping("/{institutionId}/branding")
    @Operation(summary = "Public branding for one institution")
    public ApiResponse<BrandingResponse> branding(@PathVariable Long institutionId) {
        return ApiResponse.ok(institutionService.branding(institutionId));
    }

    @GetMapping
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "Search institutions")
    public ApiResponse<PageResponse<InstitutionResponse>> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(institutionService.search(query, active, PageRequest.of(page, Math.min(size, 100))));
    }

    @GetMapping("/by-code/{code}")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "One institution by its code")
    public ApiResponse<InstitutionResponse> findByCode(@PathVariable String code) {
        return ApiResponse.ok(institutionService.findByCode(code));
    }

    @GetMapping("/{institutionId}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','INSTITUTION_STAFF')")
    @Operation(summary = "Full record for one institution")
    public ApiResponse<InstitutionResponse> findById(@PathVariable Long institutionId) {
        return ApiResponse.ok(institutionService.findById(institutionId));
    }

    @PostMapping
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "Onboard an institution")
    public ApiResponse<InstitutionResponse> create(@Valid @RequestBody InstitutionRequest request) {
        return ApiResponse.ok(institutionService.create(request), "Institution created");
    }

    @PutMapping("/{institutionId}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','INSTITUTION_STAFF')")
    @Operation(summary = "Update branding and identifier rules")
    public ApiResponse<InstitutionResponse> update(@PathVariable Long institutionId,
                                                   @Valid @RequestBody InstitutionRequest request) {
        return ApiResponse.ok(institutionService.update(institutionId, request), "Institution updated");
    }

    @PatchMapping("/{institutionId}/deactivate")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "Hide an institution from sign-up")
    public ApiResponse<InstitutionResponse> deactivate(@PathVariable Long institutionId) {
        return ApiResponse.ok(institutionService.setActive(institutionId, false), "Institution deactivated");
    }

    @PatchMapping("/{institutionId}/reactivate")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "Show an institution again")
    public ApiResponse<InstitutionResponse> reactivate(@PathVariable Long institutionId) {
        return ApiResponse.ok(institutionService.setActive(institutionId, true), "Institution reactivated");
    }
}
