package com.legacyloop.career.controller;

import com.legacyloop.career.dto.ResumeDtos.RenameRequest;
import com.legacyloop.career.dto.ResumeDtos.ResumeResponse;
import com.legacyloop.career.service.ResumeService;
import com.legacyloop.common.ApiResponse;
import com.legacyloop.common.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Resumes", description = "Upload, list, download and manage CVs")
@RestController
@RequestMapping("/api/v1/resumes")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeService resumeService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a CV; the text is extracted at this point for later AI analysis")
    public ApiResponse<ResumeResponse> upload(@AuthenticationPrincipal AuthUser user,
                                              @RequestPart("file") MultipartFile file,
                                              @RequestParam(required = false) String label) {
        return ApiResponse.ok(resumeService.upload(file, label, user.id()), "Resume uploaded");
    }

    @GetMapping
    @Operation(summary = "Your resumes")
    public ApiResponse<List<ResumeResponse>> list(@AuthenticationPrincipal AuthUser user) {
        return ApiResponse.ok(resumeService.list(user.id()));
    }

    @GetMapping("/{resumeId}")
    @Operation(summary = "One resume")
    public ApiResponse<ResumeResponse> findById(@AuthenticationPrincipal AuthUser user,
                                                @PathVariable Long resumeId) {
        return ApiResponse.ok(resumeService.findById(resumeId, user));
    }

    /** Returns the file itself, so this one is a raw {@code ResponseEntity} rather than an envelope. */
    @GetMapping("/{resumeId}/download")
    @Operation(summary = "Download the original file")
    public ResponseEntity<Resource> download(@AuthenticationPrincipal AuthUser user,
                                             @PathVariable Long resumeId) {
        if (user == null) {
            throw com.legacyloop.common.ApiException.unauthorized("Sign in to view or download resumes");
        }
        var payload = resumeService.download(resumeId, user);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + payload.filename() + "\"")
                .contentType(MediaType.parseMediaType(payload.contentType()))
                .body(payload.resource());
    }

    @PatchMapping("/{resumeId}/primary")
    @Operation(summary = "Use this one by default when applying")
    public ApiResponse<ResumeResponse> makePrimary(@AuthenticationPrincipal AuthUser user,
                                                   @PathVariable Long resumeId) {
        return ApiResponse.ok(resumeService.makePrimary(resumeId, user.id()), "Primary resume updated");
    }

    @PatchMapping("/{resumeId}/rename")
    @Operation(summary = "Rename a resume")
    public ApiResponse<ResumeResponse> rename(@AuthenticationPrincipal AuthUser user,
                                              @PathVariable Long resumeId,
                                              @Valid @RequestBody RenameRequest request) {
        return ApiResponse.ok(resumeService.rename(resumeId, user.id(), request.label()), "Resume renamed");
    }

    @DeleteMapping("/{resumeId}")
    @Operation(summary = "Delete a resume and its stored file")
    public ApiResponse<Void> delete(@AuthenticationPrincipal AuthUser user, @PathVariable Long resumeId) {
        resumeService.delete(resumeId, user.id());
        return ApiResponse.message("Resume deleted");
    }
}

