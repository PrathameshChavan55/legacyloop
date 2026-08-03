package com.legacyloop.auth.controller;

import com.legacyloop.auth.repository.UserRepository;
import com.legacyloop.common.dto.ApiResponse;
import com.legacyloop.common.dto.PageResponse;
import com.legacyloop.common.dto.UserSummaryDto;
import com.legacyloop.common.enums.Role;
import com.legacyloop.common.enums.UserStatus;
import com.legacyloop.common.security.SecurityContextUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * The people directory - who you can send a connection request to.
 *
 * <p>Separate from AdminUserController, which lists accounts for administration. This one is
 * for any signed-in user and is deliberately thin: name, role, headline, avatar. No email,
 * no status, no identifier. A student browsing for alumni has no business seeing who is
 * suspended or what anyone's contact details are.</p>
 */
@RestController
@RequestMapping("/api/v1/directory")
@RequiredArgsConstructor
@Tag(name = "Directory", description = "Find people to connect with")
public class DirectoryController {

    private final UserRepository userRepository;

    @GetMapping
    @Operation(summary = "Browse and search people",
            description = "Active accounts only. Administrators and you are excluded.")
    public ResponseEntity<ApiResponse<PageResponse<UserSummaryDto>>> browse(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Long me = SecurityContextUtil.requireUserId();
        Page<com.legacyloop.auth.entity.User> result = userRepository.searchDirectory(
                q == null || q.isBlank() ? null : q.trim().toLowerCase(),
                Role.from(role),
                me,
                UserStatus.ACTIVE,
                PageRequest.of(page, Math.min(size, 50), Sort.by("fullName")));

        List<UserSummaryDto> people = result.getContent().stream()
                .map(user -> new UserSummaryDto(
                        user.getId(),
                        user.getFullName(),
                        // Email is deliberately omitted: the directory is for finding
                        // someone, not for harvesting contact details.
                        null,
                        user.getRoles().stream().findFirst()
                                .map(role1 -> role1.getName().name()).orElse(null),
                        user.getAvatarUrl(),
                        headlineFor(user),
                        user.isPremiumActive(),
                        true))
                .toList();

        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(
                people, result.getNumber(), result.getSize(), result.getTotalElements())));
    }

    /** Falls back to designation at company so an alumni row is never blank. */
    private String headlineFor(com.legacyloop.auth.entity.User user) {
        if (user.getHeadline() != null && !user.getHeadline().isBlank()) {
            return user.getHeadline();
        }
        if (user.getDesignation() != null && user.getCompanyName() != null) {
            return user.getDesignation() + " at " + user.getCompanyName();
        }
        return user.getBatchId();
    }
}
