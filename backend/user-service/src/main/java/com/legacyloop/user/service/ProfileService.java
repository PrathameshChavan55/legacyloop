package com.legacyloop.user.service;

import com.legacyloop.common.ApiException;
import com.legacyloop.common.PageResponse;
import com.legacyloop.common.Roles;
import com.legacyloop.user.dto.ProfileDtos.AlumniProfileRequest;
import com.legacyloop.user.dto.ProfileDtos.AlumniProfileResponse;
import com.legacyloop.user.dto.ProfileDtos.ProfileSummary;
import com.legacyloop.user.dto.ProfileDtos.SkillResponse;
import com.legacyloop.user.dto.ProfileDtos.StudentProfileRequest;
import com.legacyloop.user.dto.ProfileDtos.StudentProfileResponse;
import com.legacyloop.user.entity.AlumniProfile;
import com.legacyloop.user.entity.Skill;
import com.legacyloop.user.entity.StudentProfile;
import com.legacyloop.user.entity.User;
import com.legacyloop.user.repository.AlumniProfileRepository;
import com.legacyloop.user.repository.SkillRepository;
import com.legacyloop.user.repository.StudentProfileRepository;
import com.legacyloop.user.repository.UserRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.web.multipart.MultipartFile;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Student and alumni profiles, the directory, and the shared skill vocabulary.
 *
 * <p>Student and alumni profiles are different enough to stay separate entities, but their
 * read paths share {@link #summaries}: fetch the page, then resolve names and academic units in
 * one query each instead of one per row.
 */
@Service
@RequiredArgsConstructor
public class ProfileService {

    private final StudentProfileRepository studentProfiles;
    private final AlumniProfileRepository alumniProfiles;
    private final SkillRepository skills;
    private final UserRepository users;
    private final AcademicService academics;

    /** Called once, when an account is verified or created by an admin. */
    @Transactional
    public void createEmptyProfile(User user) {
        if (user.hasRole(Roles.ROLE_STUDENT) && studentProfiles.findByUserId(user.getId()).isEmpty()) {
            studentProfiles.save(StudentProfile.builder()
                    .userId(user.getId())
                    .institutionId(user.getInstitutionId())
                    .build());
        } else if (user.hasRole(Roles.ROLE_ALUMNI) && alumniProfiles.findByUserId(user.getId()).isEmpty()) {
            alumniProfiles.save(AlumniProfile.builder()
                    .userId(user.getId())
                    .institutionId(user.getInstitutionId())
                    .build());
        }
    }

    @Transactional
    public void deleteProfileByUserId(Long userId) {
        studentProfiles.findByUserId(userId).ifPresent(studentProfiles::delete);
        alumniProfiles.findByUserId(userId).ifPresent(alumniProfiles::delete);
    }

    /* ------------------------------------------------------------------ student profile */

    @Transactional(readOnly = true)
    public StudentProfileResponse student(Long userId) {
        return toStudentResponse(loadStudent(userId));
    }

    @Transactional
    public StudentProfileResponse updateStudent(Long userId, StudentProfileRequest request) {
        StudentProfile profile = loadStudent(userId);

        profile.setDepartmentId(request.departmentId());
        profile.setProgramId(request.programId());
        profile.setBranchId(request.branchId());
        profile.setBatchId(request.batchId());
        profile.setCgpa(request.cgpa());
        profile.setTenthPercentage(request.tenthPercentage());
        profile.setTwelfthPercentage(request.twelfthPercentage());
        profile.setGraduationDegree(request.graduationDegree());
        profile.setGraduationYear(request.graduationYear());
        profile.setBacklogs(request.backlogs() == null ? 0 : request.backlogs());
        profile.setHeadline(request.headline());
        profile.setAbout(request.about());
        profile.setLocation(request.location());
        profile.setProfilePhotoUrl(request.profilePhotoUrl());
        profile.setLinkedinUrl(request.linkedinUrl());
        profile.setGithubUrl(request.githubUrl());
        profile.setPortfolioUrl(request.portfolioUrl());
        profile.setSkills(registerSkills(request.skills()));
        if (request.profileVisible() != null) {
            profile.setProfileVisible(request.profileVisible());
        }
        if (request.openToWork() != null) {
            profile.setOpenToWork(request.openToWork());
        }
        return toStudentResponse(profile);
    }

    /* ------------------------------------------------------------------- alumni profile */

    @Transactional(readOnly = true)
    public AlumniProfileResponse alumni(Long userId) {
        return toAlumniResponse(loadAlumni(userId));
    }

    @Transactional
    public AlumniProfileResponse updateAlumni(Long userId, AlumniProfileRequest request) {
        AlumniProfile profile = loadAlumni(userId);

        profile.setProgramId(request.programId());
        profile.setBatchId(request.batchId());
        profile.setGraduationYear(request.graduationYear());
        profile.setCurrentCompany(request.currentCompany());
        profile.setCurrentDesignation(request.currentDesignation());
        profile.setCurrentLocation(request.currentLocation());
        profile.setIndustry(request.industry());
        profile.setTotalExperienceMonths(request.totalExperienceMonths());
        profile.setPreviousCompanies(request.previousCompanies());
        profile.setHeadline(request.headline());
        profile.setAbout(request.about());
        profile.setProfilePhotoUrl(request.profilePhotoUrl());
        profile.setLinkedinUrl(request.linkedinUrl());
        profile.setGithubUrl(request.githubUrl());
        profile.setPortfolioUrl(request.portfolioUrl());
        profile.setSkills(registerSkills(request.skills()));
        if (request.willingToRefer() != null) {
            profile.setWillingToRefer(request.willingToRefer());
        }
        if (request.availableForMentorship() != null) {
            profile.setAvailableForMentorship(request.availableForMentorship());
        }
        if (request.maxReferralsPerMonth() != null) {
            profile.setMaxReferralsPerMonth(request.maxReferralsPerMonth());
        }
        profile.setMentorshipAreas(request.mentorshipAreas());
        if (request.profileVisible() != null) {
            profile.setProfileVisible(request.profileVisible());
        }
        return toAlumniResponse(profile);
    }

    /* ----------------------------------------------------------------------- directory */

    @Transactional(readOnly = true)
    public PageResponse<ProfileSummary> browseStudents(Long excludeUserId, Long institutionId, Long batchId, Boolean openToWork,
                                                       Pageable pageable) {
        Page<StudentProfile> page = studentProfiles.browse(excludeUserId, institutionId, batchId, openToWork, pageable);
        Map<Long, String> names = namesOf(page.getContent().stream().map(StudentProfile::getUserId).toList());
        Map<Long, String> batches = academics.namesOf(
                page.getContent().stream().map(StudentProfile::getBatchId).toList());

        return PageResponse.of(page, profile -> ProfileSummary.ofStudent(profile,
                names.getOrDefault(profile.getUserId(), "Student"),
                batches.get(profile.getBatchId())));
    }

    @Transactional(readOnly = true)
    public PageResponse<ProfileSummary> browseAlumni(Long excludeUserId, Long institutionId, String company, boolean mentorsOnly,
                                                     Pageable pageable) {
        String normalisedCompany = company == null || company.isBlank() ? null
                : company.trim().toLowerCase(Locale.ROOT);
        Page<AlumniProfile> page = alumniProfiles.browse(excludeUserId, institutionId, normalisedCompany, mentorsOnly, pageable);
        Map<Long, String> names = namesOf(page.getContent().stream().map(AlumniProfile::getUserId).toList());

        return PageResponse.of(page, profile -> ProfileSummary.ofAlumni(profile,
                names.getOrDefault(profile.getUserId(), "Alumnus")));
    }

    /** Alumni at a company who have said they will refer. Used by the referral picker. */
    @Transactional(readOnly = true)
    public List<ProfileSummary> referrersAt(String company) {
        if (company == null || company.isBlank()) {
            return List.of();
        }
        List<AlumniProfile> found = alumniProfiles.findReferrersAtCompany(company.trim());
        Map<Long, String> names = namesOf(found.stream().map(AlumniProfile::getUserId).toList());
        return found.stream()
                .map(profile -> ProfileSummary.ofAlumni(profile,
                        names.getOrDefault(profile.getUserId(), "Alumnus")))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<String> alumniCompanies() {
        return alumniProfiles.findDistinctCompanies();
    }

    /* -------------------------------------------------------------------------- skills */

    @Transactional(readOnly = true)
    public List<SkillResponse> suggestSkills(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        return skills.findTop10ByNameContainingIgnoreCaseOrderByUsageCountDesc(query.trim()).stream()
                .map(SkillResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<SkillResponse> popularSkills(int limit) {
        return skills.findByOrderByUsageCountDesc(Pageable.ofSize(Math.min(limit, 50))).stream()
                .map(SkillResponse::from).toList();
    }

    @Transactional
    public SkillResponse approveSkill(Long skillId) {
        Skill skill = skills.findById(skillId).orElseThrow(() -> ApiException.notFound("Skill", skillId));
        skill.setApproved(true);
        return SkillResponse.from(skill);
    }

    /**
     * Records the skills a profile claims, creating any the vocabulary has not seen. New names
     * start unapproved so autocomplete stays open without an administrator gate-keeping every
     * technology.
     */
    private Set<String> registerSkills(Set<String> names) {
        Set<String> cleaned = new LinkedHashSet<>();
        for (String raw : names) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            String name = raw.trim();
            Skill skill = skills.findByNameIgnoreCase(name)
                    .orElseGet(() -> skills.save(Skill.builder().name(name).build()));
            skill.setUsageCount(skill.getUsageCount() + 1);
            cleaned.add(skill.getName());
        }
        return cleaned;
    }

    /* --------------------------------------------------------------------- shared reads */

    /** Counts for the placement dashboard. */
    @Transactional(readOnly = true)
    public long placedStudentCount() {
        return studentProfiles.countByPlacedTrue();
    }

    @Transactional(readOnly = true)
    public long mentorCount() {
        return alumniProfiles.countByAvailableForMentorshipTrue();
    }

    /** Marks a student as placed. Called by career-service when an offer is recorded. */
    @Transactional
    public void markPlaced(Long userId, String company, java.math.BigDecimal packageValue) {
        studentProfiles.findByUserId(userId).ifPresent(profile -> {
            profile.setPlaced(true);
            profile.setPlacedCompany(company);
            profile.setPlacedPackage(packageValue);
            profile.setOpenToWork(false);
        });
    }

    /** The academic snapshot career-service needs to judge eligibility for a job. */
    @Transactional(readOnly = true)
    public Map<String, Object> eligibilitySnapshot(Long userId) {
        return studentProfiles.findByUserId(userId)
                .<Map<String, Object>>map(profile -> {
                    java.util.HashMap<String, Object> snapshot = new java.util.HashMap<>();
                    snapshot.put("cgpa", profile.getCgpa());
                    snapshot.put("backlogs", profile.getBacklogs());
                    snapshot.put("batchId", profile.getBatchId());
                    snapshot.put("batchName", profile.getBatchId() == null ? null
                            : academics.namesOf(List.of(profile.getBatchId())).get(profile.getBatchId()));
                    snapshot.put("placed", profile.isPlaced());
                    return snapshot;
                })
                .orElse(Map.of());
    }

    private Map<Long, String> namesOf(List<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return users.findByIdIn(userIds).stream()
                .collect(Collectors.toMap(User::getId, User::fullName, (first, second) -> first));
    }

    private StudentProfile loadStudent(Long userId) {
        return studentProfiles.findByUserId(userId)
                .orElseThrow(() -> ApiException.notFound("Student profile for user", userId));
    }

    private AlumniProfile loadAlumni(Long userId) {
        return alumniProfiles.findByUserId(userId)
                .orElseThrow(() -> ApiException.notFound("Alumni profile for user", userId));
    }

    private StudentProfileResponse toStudentResponse(StudentProfile profile) {
        User user = users.findById(profile.getUserId()).orElseThrow(
                () -> ApiException.notFound("User", profile.getUserId()));
        Map<Long, String> unitNames = academics.namesOf(List.of(
                nullSafe(profile.getDepartmentId()), nullSafe(profile.getProgramId()),
                nullSafe(profile.getBranchId()), nullSafe(profile.getBatchId())));

        return new StudentProfileResponse(profile.getId(), profile.getUserId(), user.fullName(),
                user.getEmail(), user.getStudentIdentifier(), profile.getInstitutionId(),
                profile.getDepartmentId(), unitNames.get(profile.getDepartmentId()),
                profile.getProgramId(), unitNames.get(profile.getProgramId()),
                profile.getBranchId(), unitNames.get(profile.getBranchId()),
                profile.getBatchId(), unitNames.get(profile.getBatchId()),
                profile.getCgpa(), profile.getTenthPercentage(), profile.getTwelfthPercentage(),
                profile.getGraduationDegree(), profile.getGraduationYear(), profile.getBacklogs(),
                profile.getHeadline(), profile.getAbout(), profile.getLocation(),
                profile.getProfilePhotoUrl(), profile.getLinkedinUrl(), profile.getGithubUrl(),
                profile.getPortfolioUrl(), Set.copyOf(profile.getSkills()), profile.isPlaced(),
                profile.getPlacedCompany(), profile.getPlacedPackage(), profile.isProfileVisible(),
                profile.isOpenToWork(), profile.completeness(), profile.getUpdatedAt());
    }

    private AlumniProfileResponse toAlumniResponse(AlumniProfile profile) {
        User user = users.findById(profile.getUserId()).orElseThrow(
                () -> ApiException.notFound("User", profile.getUserId()));
        Map<Long, String> unitNames = academics.namesOf(List.of(
                nullSafe(profile.getProgramId()), nullSafe(profile.getBatchId())));

        return new AlumniProfileResponse(profile.getId(), profile.getUserId(), user.fullName(),
                user.getEmail(), profile.getInstitutionId(), profile.getProgramId(),
                unitNames.get(profile.getProgramId()), profile.getBatchId(),
                unitNames.get(profile.getBatchId()), profile.getGraduationYear(),
                profile.getCurrentCompany(), profile.getCurrentDesignation(), profile.getCurrentLocation(),
                profile.getIndustry(), profile.getTotalExperienceMonths(), profile.experienceLabel(),
                profile.getPreviousCompanies(), profile.getHeadline(), profile.getAbout(),
                profile.getProfilePhotoUrl(), profile.getLinkedinUrl(), profile.getGithubUrl(),
                profile.getPortfolioUrl(), Set.copyOf(profile.getSkills()), profile.isWillingToRefer(),
                profile.isAvailableForMentorship(), profile.getMaxReferralsPerMonth(),
                profile.getMentorshipAreas(), profile.isProfileVisible(), profile.getUpdatedAt());
    }

    /** {@code List.of} rejects nulls, and half these ids are legitimately null. */
    private static Long nullSafe(Long value) {
        return value == null ? -1L : value;
    }

    @Transactional
    public String uploadPhoto(Long userId, MultipartFile file) {
        if (file.isEmpty()) {
            throw ApiException.badRequest("Please choose a file to upload");
        }
        String contentType = file.getContentType();
        if (contentType == null || (!contentType.equals("image/jpeg") && !contentType.equals("image/png") && !contentType.equals("image/jpg"))) {
            throw ApiException.badRequest("Please upload a JPEG or PNG image");
        }

        try {
            Path dir = Paths.get("uploads/photos");
            Files.createDirectories(dir);

            String originalFilename = file.getOriginalFilename();
            String extension = ".jpg";
            if (originalFilename != null && originalFilename.toLowerCase().endsWith(".png")) {
                extension = ".png";
            }

            String filename = "photo_" + userId + "_" + System.currentTimeMillis() + extension;
            Path filePath = dir.resolve(filename);
            Files.copy(file.getInputStream(), filePath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            String photoUrl = "/api/v1/profiles/photos/" + filename;

            User user = users.findById(userId).orElseThrow(() -> ApiException.notFound("User", userId));
            if (user.hasRole(Roles.ROLE_STUDENT)) {
                StudentProfile profile = studentProfiles.findByUserId(userId)
                        .orElseThrow(() -> ApiException.notFound("Student Profile", userId));
                profile.setProfilePhotoUrl(photoUrl);
                studentProfiles.save(profile);
            } else if (user.hasRole(Roles.ROLE_ALUMNI)) {
                AlumniProfile profile = alumniProfiles.findByUserId(userId)
                        .orElseThrow(() -> ApiException.notFound("Alumni Profile", userId));
                profile.setProfilePhotoUrl(photoUrl);
                alumniProfiles.save(profile);
            }

            return photoUrl;
        } catch (IOException e) {
            throw ApiException.badRequest("Could not store photo: " + e.getMessage());
        }
    }

    public Resource loadPhotoAsResource(String filename) {
        try {
            Path file = Paths.get("uploads/photos").resolve(filename).normalize();
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() || resource.isReadable()) {
                return resource;
            } else {
                throw ApiException.notFound("Photo", filename);
            }
        } catch (Exception e) {
            throw ApiException.notFound("Photo", filename);
        }
    }
}
