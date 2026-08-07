package com.legacyloop.user.controller;

import com.legacyloop.common.ApiResponse;
import com.legacyloop.common.AuthUser;
import com.legacyloop.common.PageResponse;
import com.legacyloop.user.dto.ProfileDtos.AlumniProfileRequest;
import com.legacyloop.user.dto.ProfileDtos.AlumniProfileResponse;
import com.legacyloop.user.dto.ProfileDtos.ProfileSummary;
import com.legacyloop.user.dto.ProfileDtos.SkillResponse;
import com.legacyloop.user.dto.ProfileDtos.StudentProfileRequest;
import com.legacyloop.user.dto.ProfileDtos.StudentProfileResponse;
import com.legacyloop.user.service.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Profiles", description = "Student and alumni profiles, the directory and skills")
@RestController
@RequestMapping("/api/v1/profiles")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping("/me/student")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Your student profile")
    public ApiResponse<StudentProfileResponse> myStudentProfile(@AuthenticationPrincipal AuthUser user) {
        return ApiResponse.ok(profileService.student(user.id()));
    }

    @PutMapping("/me/student")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Update your student profile")
    public ApiResponse<StudentProfileResponse> updateMyStudentProfile(
            @AuthenticationPrincipal AuthUser user, @Valid @RequestBody StudentProfileRequest request) {
        return ApiResponse.ok(profileService.updateStudent(user.id(), request), "Profile saved");
    }

    @GetMapping("/me/alumni")
    @PreAuthorize("hasRole('ALUMNI')")
    @Operation(summary = "Your alumni profile")
    public ApiResponse<AlumniProfileResponse> myAlumniProfile(@AuthenticationPrincipal AuthUser user) {
        return ApiResponse.ok(profileService.alumni(user.id()));
    }

    @PutMapping("/me/alumni")
    @PreAuthorize("hasRole('ALUMNI')")
    @Operation(summary = "Update your alumni profile")
    public ApiResponse<AlumniProfileResponse> updateMyAlumniProfile(
            @AuthenticationPrincipal AuthUser user, @Valid @RequestBody AlumniProfileRequest request) {
        return ApiResponse.ok(profileService.updateAlumni(user.id(), request), "Profile saved");
    }

    @GetMapping("/students")
    @Operation(summary = "Browse student profiles")
    public ApiResponse<PageResponse<ProfileSummary>> browseStudents(
            @AuthenticationPrincipal AuthUser user,
            @RequestParam(required = false) Long institutionId,
            @RequestParam(required = false) Long batchId,
            @RequestParam(required = false) Boolean openToWork,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long excludeUserId = user != null ? user.id() : null;
        return ApiResponse.ok(profileService.browseStudents(excludeUserId, institutionId, batchId, openToWork,
                PageRequest.of(page, Math.min(size, 50))));
    }

    @GetMapping("/alumni/companies")
    @Operation(summary = "Distinct companies alumni work at, for the directory filter")
    public ApiResponse<List<String>> alumniCompanies() {
        return ApiResponse.ok(profileService.alumniCompanies());
    }

    @GetMapping("/alumni")
    @Operation(summary = "Browse alumni profiles")
    public ApiResponse<PageResponse<ProfileSummary>> browseAlumni(
            @AuthenticationPrincipal AuthUser user,
            @RequestParam(required = false) Long institutionId,
            @RequestParam(required = false) String company,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long excludeUserId = user != null ? user.id() : null;
        return ApiResponse.ok(profileService.browseAlumni(excludeUserId, institutionId, company, false,
                PageRequest.of(page, Math.min(size, 50))));
    }

    @GetMapping("/mentors")
    @Operation(summary = "Alumni offering mentorship")
    public ApiResponse<PageResponse<ProfileSummary>> browseMentors(
            @AuthenticationPrincipal AuthUser user,
            @RequestParam(required = false) Long institutionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long excludeUserId = user != null ? user.id() : null;
        return ApiResponse.ok(profileService.browseAlumni(excludeUserId, institutionId, null, true,
                PageRequest.of(page, Math.min(size, 50))));
    }

    @GetMapping("/students/{userId}")
    @Operation(summary = "Someone else's student profile")
    public ApiResponse<StudentProfileResponse> viewStudent(@PathVariable Long userId) {
        return ApiResponse.ok(profileService.student(userId));
    }

    @GetMapping("/alumni/{userId}")
    @Operation(summary = "Someone else's alumni profile")
    public ApiResponse<AlumniProfileResponse> viewAlumni(@PathVariable Long userId) {
        return ApiResponse.ok(profileService.alumni(userId));
    }

    @GetMapping("/skills/suggest")
    @Operation(summary = "Autocomplete over the skill vocabulary")
    public ApiResponse<List<SkillResponse>> suggestSkills(@RequestParam String query) {
        return ApiResponse.ok(profileService.suggestSkills(query));
    }

    @GetMapping("/skills/popular")
    @Operation(summary = "The most-claimed skills")
    public ApiResponse<List<SkillResponse>> popularSkills(@RequestParam(defaultValue = "20") int limit) {
        return ApiResponse.ok(profileService.popularSkills(limit));
    }

    @PatchMapping("/skills/{skillId}/approve")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "Mark a user-created skill as part of the official vocabulary")
    public ApiResponse<SkillResponse> approveSkill(@PathVariable Long skillId) {
        return ApiResponse.ok(profileService.approveSkill(skillId), "Skill approved");
    }

    @PostMapping("/me/photo")
    @Operation(summary = "Upload a profile photo")
    public ApiResponse<String> uploadPhoto(@AuthenticationPrincipal AuthUser user,
                                           @RequestParam("file") MultipartFile file) {
        String url = profileService.uploadPhoto(user.id(), file);
        return ApiResponse.ok(url, "Photo uploaded successfully");
    }

    @GetMapping("/photos/{filename}")
    @Operation(summary = "Serve a profile photo")
    public ResponseEntity<Resource> servePhoto(@PathVariable String filename) {
        Resource fileResource = profileService.loadPhotoAsResource(filename);
        String contentType = "image/jpeg";
        if (filename.toLowerCase().endsWith(".png")) {
            contentType = "image/png";
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .body(fileResource);
    }
}
