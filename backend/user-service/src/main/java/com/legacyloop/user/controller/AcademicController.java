package com.legacyloop.user.controller;

import com.legacyloop.common.ApiResponse;
import com.legacyloop.user.dto.AcademicDtos.AcademicRequest;
import com.legacyloop.user.dto.AcademicDtos.AcademicResponse;
import com.legacyloop.user.entity.AcademicUnit.Type;
import com.legacyloop.user.service.AcademicService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
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

/**
 * Departments, programs, branches and batches.
 *
 * <p>The type is a path segment, so {@code /academics/departments} and {@code /academics/batches}
 * are the same eight methods rather than two copies of them. The original had four controllers
 * with 32 methods between them saying the same thing.
 */
@Tag(name = "Academics", description = "Departments, programs, branches and batches")
@RestController
@RequestMapping("/api/v1/academics/{type}")
@RequiredArgsConstructor
public class AcademicController {

    private final AcademicService academicService;

    @GetMapping
    @Operation(summary = "List units of one type, optionally filtered by institution or parent")
    public ApiResponse<List<AcademicResponse>> list(
            @Parameter(description = "departments, programs, branches or batches") @PathVariable String type,
            @RequestParam(required = false) Long institutionId,
            @RequestParam(required = false) Long parentId,
            @RequestParam(required = false) Boolean active) {
        return ApiResponse.ok(academicService.list(Type.fromSlug(type), institutionId, parentId, active));
    }

    @GetMapping("/placement-open")
    @Operation(summary = "Batches currently open for placement drives")
    public ApiResponse<List<AcademicResponse>> placementOpen(@PathVariable String type) {
        Type.fromSlug(type);
        return ApiResponse.ok(academicService.placementOpenBatches());
    }

    @GetMapping("/{id}")
    @Operation(summary = "One unit")
    public ApiResponse<AcademicResponse> findById(@PathVariable String type, @PathVariable Long id) {
        return ApiResponse.ok(academicService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','INSTITUTION_STAFF')")
    @Operation(summary = "Create a unit")
    public ApiResponse<AcademicResponse> create(@PathVariable String type,
                                                @Valid @RequestBody AcademicRequest request) {
        return ApiResponse.ok(academicService.create(Type.fromSlug(type), request), "Created");
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','INSTITUTION_STAFF')")
    @Operation(summary = "Update a unit")
    public ApiResponse<AcademicResponse> update(@PathVariable String type, @PathVariable Long id,
                                                @Valid @RequestBody AcademicRequest request) {
        return ApiResponse.ok(academicService.update(id, request), "Updated");
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','INSTITUTION_STAFF')")
    @Operation(summary = "Retire a unit without deleting the history that points at it")
    public ApiResponse<AcademicResponse> deactivate(@PathVariable String type, @PathVariable Long id) {
        return ApiResponse.ok(academicService.setActive(id, false), "Deactivated");
    }

    @PatchMapping("/{id}/reactivate")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','INSTITUTION_STAFF')")
    @Operation(summary = "Bring a unit back")
    public ApiResponse<AcademicResponse> reactivate(@PathVariable String type, @PathVariable Long id) {
        return ApiResponse.ok(academicService.setActive(id, true), "Reactivated");
    }
}
