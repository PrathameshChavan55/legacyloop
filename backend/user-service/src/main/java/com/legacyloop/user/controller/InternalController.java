package com.legacyloop.user.controller;

import com.legacyloop.common.ApiResponse;
import com.legacyloop.user.dto.ProfileDtos.ProfileSummary;
import com.legacyloop.user.dto.UserDtos.UserSummary;
import com.legacyloop.user.service.ProfileService;
import com.legacyloop.user.service.UserAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Service-to-service reads. Career-service needs a name for an applicant id; social-service needs
 * names for a page of posts. These are on {@code /internal} and are not exposed by Nginx, so they
 * are reachable inside the compose network only.
 */
@Tag(name = "Internal", description = "Called by career-service and social-service, not by the browser")
@RestController
@RequestMapping("/internal/v1")
@RequiredArgsConstructor
public class InternalController {

    private final UserAdminService userAdminService;
    private final ProfileService profileService;

    @GetMapping("/users/{userId}")
    @Operation(summary = "One account summary")
    public ApiResponse<UserSummary> user(@PathVariable Long userId) {
        return ApiResponse.ok(userAdminService.summary(userId));
    }

    @PostMapping("/users/bulk")
    @Operation(summary = "Many account summaries in one call — this is what avoids N+1 across services")
    public ApiResponse<Map<Long, UserSummary>> users(@RequestBody List<Long> userIds) {
        return ApiResponse.ok(userAdminService.summaries(userIds));
    }

    @GetMapping("/profiles/{userId}/eligibility")
    @Operation(summary = "CGPA, backlogs and batch — what a job's eligibility rules are checked against")
    public ApiResponse<Map<String, Object>> eligibility(@PathVariable Long userId) {
        return ApiResponse.ok(profileService.eligibilitySnapshot(userId));
    }

    @GetMapping("/profiles/referrers")
    @Operation(summary = "Alumni at a company who are willing to refer")
    public ApiResponse<List<ProfileSummary>> referrers(@RequestParam String company) {
        return ApiResponse.ok(profileService.referrersAt(company));
    }

    @PostMapping("/profiles/{userId}/placed")
    @Operation(summary = "Record an offer on the student's profile")
    public ApiResponse<Void> markPlaced(@PathVariable Long userId,
                                        @RequestParam String company,
                                        @RequestParam(required = false) BigDecimal packageValue) {
        profileService.markPlaced(userId, company, packageValue);
        return ApiResponse.message("Recorded");
    }

    @GetMapping("/profiles/stats")
    @Operation(summary = "Counts the placement dashboard needs")
    public ApiResponse<Map<String, Long>> stats() {
        return ApiResponse.ok(Map.of(
                "placedStudents", profileService.placedStudentCount(),
                "mentors", profileService.mentorCount()));
    }
}
