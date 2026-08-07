package com.legacyloop.career.service;

import com.legacyloop.common.ApiException;
import com.legacyloop.common.AuthUser;
import com.legacyloop.common.PageResponse;
import com.legacyloop.career.dto.CompanyDtos.CompanyRequest;
import com.legacyloop.career.dto.CompanyDtos.CompanyResponse;
import com.legacyloop.career.dto.JobDtos.EligibilityCheck;
import com.legacyloop.career.dto.JobDtos.JobDetail;
import com.legacyloop.career.dto.JobDtos.JobRequest;
import com.legacyloop.career.dto.JobDtos.JobSummary;
import com.legacyloop.career.entity.Company;
import com.legacyloop.career.entity.Enums.JobStatus;
import com.legacyloop.career.entity.Enums.JobType;
import com.legacyloop.career.entity.Enums.WorkMode;
import com.legacyloop.career.entity.Job;
import com.legacyloop.career.repository.CompanyRepository;
import com.legacyloop.career.repository.JobApplicationRepository;
import com.legacyloop.career.repository.JobRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Postings and the employers behind them.
 *
 * <p>Companies are here rather than in their own service because a company exists only as
 * something to post a job against — separating them meant two services, two repositories and a
 * join across them for every list view.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JobService {

    private final JobRepository jobs;
    private final CompanyRepository companies;
    private final JobApplicationRepository applications;
    private final UserClient userClient;

    /* ------------------------------------------------------------------------- companies */

    @Transactional(readOnly = true)
    public PageResponse<CompanyResponse> searchCompanies(String query, Boolean verified, Boolean active,
                                                         Pageable pageable) {
        String like = like(query);
        return PageResponse.of(companies.search(like, verified, active, pageable), CompanyResponse::from);
    }

    @Transactional(readOnly = true)
    public List<CompanyResponse> activeCompanies() {
        return companies.findByActiveTrueOrderByNameAsc().stream().map(CompanyResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public CompanyResponse company(Long companyId) {
        return CompanyResponse.from(loadCompany(companyId));
    }

    @Transactional
    public CompanyResponse createCompany(CompanyRequest request) {
        if (companies.existsByNameIgnoreCase(request.name())) {
            throw ApiException.conflict("That company is already on the platform");
        }
        Company company = new Company();
        applyCompany(company, request);
        company.setName(request.name());
        return CompanyResponse.from(companies.save(company));
    }

    @Transactional
    public CompanyResponse updateCompany(Long companyId, CompanyRequest request) {
        Company company = loadCompany(companyId);
        applyCompany(company, request);
        company.setName(request.name());
        return CompanyResponse.from(company);
    }

    @Transactional
    public CompanyResponse setCompanyVerified(Long companyId, boolean verified) {
        Company company = loadCompany(companyId);
        company.setVerified(verified);
        return CompanyResponse.from(company);
    }

    @Transactional
    public CompanyResponse setCompanyActive(Long companyId, boolean active) {
        Company company = loadCompany(companyId);
        company.setActive(active);
        return CompanyResponse.from(company);
    }

    /* ------------------------------------------------------------------------------ jobs */

    /** The public board: open postings only. */
    @Transactional(readOnly = true)
    public PageResponse<JobSummary> search(String query, String jobType, String workMode, Long companyId,
                                           Pageable pageable) {
        return PageResponse.of(jobs.search(like(query), JobStatus.OPEN, enumOrNull(JobType.class, jobType),
                enumOrNull(WorkMode.class, workMode), companyId, null, null, pageable), JobSummary::from);
    }

    /** The staff console: any status, including drafts. */
    @Transactional(readOnly = true)
    public PageResponse<JobSummary> manage(String query, String status, Long companyId, Long institutionId,
                                           Pageable pageable) {
        return PageResponse.of(jobs.search(like(query), enumOrNull(JobStatus.class, status), null, null,
                companyId, institutionId, null, pageable), JobSummary::from);
    }

    /** Postings created by the caller. */
    @Transactional(readOnly = true)
    public PageResponse<JobSummary> mine(Long userId, String status, Pageable pageable) {
        return PageResponse.of(jobs.search(null, enumOrNull(JobStatus.class, status), null, null, null, null,
                userId, pageable), JobSummary::from);
    }

    /**
     * The detail view. Viewing bumps the counter, and for a student the response also carries the
     * eligibility verdict and their existing application id — so the page needs one request, not
     * three.
     */
    @Transactional
    public JobDetail detail(Long jobId, AuthUser viewer) {
        Job job = load(jobId);
        job.setViewCount(job.getViewCount() + 1);

        EligibilityCheck eligibility = viewer.hasRole("STUDENT")
                ? checkEligibility(job, viewer.id()) : EligibilityCheck.notApplicable();
        Long myApplicationId = applications.findByJobIdAndApplicantUserId(jobId, viewer.id())
                .map(application -> application.getId())
                .orElse(null);

        return JobDetail.from(job, userClient.name(job.getPostedByUserId()), eligibility, myApplicationId);
    }

    @Transactional
    public JobDetail create(JobRequest request, AuthUser author) {
        Job job = Job.builder()
                .company(loadCompany(request.companyId()))
                .postedByUserId(author.id())
                .institutionId(author.institutionId())
                .status(JobStatus.DRAFT)
                .build();
        apply(job, request);
        Job saved = jobs.save(job);
        log.info("User {} created job {}", author.id(), saved.getId());
        return JobDetail.from(saved, author.fullName(), EligibilityCheck.notApplicable(), null);
    }

    @Transactional
    public JobDetail update(Long jobId, JobRequest request, AuthUser editor) {
        Job job = load(jobId);
        requireOwnership(job, editor);
        job.setCompany(loadCompany(request.companyId()));
        apply(job, request);
        return JobDetail.from(job, userClient.name(job.getPostedByUserId()),
                EligibilityCheck.notApplicable(), null);
    }

    /** Draft to open. A job is only visible on the board once it is published. */
    @Transactional
    public JobDetail publish(Long jobId, AuthUser editor) {
        Job job = load(jobId);
        requireOwnership(job, editor);
        if (job.getStatus() == JobStatus.OPEN) {
            throw ApiException.badRequest("That job is already open");
        }
        job.setStatus(JobStatus.OPEN);
        job.setPublishedAt(Instant.now());
        return JobDetail.from(job, editor.fullName(), EligibilityCheck.notApplicable(), null);
    }

    @Transactional
    public JobDetail close(Long jobId, AuthUser editor) {
        Job job = load(jobId);
        requireOwnership(job, editor);
        job.setStatus(JobStatus.CLOSED);
        return JobDetail.from(job, editor.fullName(), EligibilityCheck.notApplicable(), null);
    }

    @Transactional
    public JobDetail reopen(Long jobId, AuthUser editor) {
        Job job = load(jobId);
        requireOwnership(job, editor);
        job.setStatus(JobStatus.OPEN);
        if (job.getPublishedAt() == null) {
            job.setPublishedAt(Instant.now());
        }
        return JobDetail.from(job, editor.fullName(), EligibilityCheck.notApplicable(), null);
    }

    /**
     * Checks a student against the job's rules.
     *
     * <p>Returns every failed rule rather than the first one, because "you need 7.0 CGPA" and
     * "your batch is not eligible" are both worth knowing at once.
     */
    @Transactional(readOnly = true)
    public EligibilityCheck checkEligibility(Job job, Long studentUserId) {
        Map<String, Object> snapshot = userClient.eligibilitySnapshot(studentUserId);
        if (snapshot.isEmpty()) {
            return new EligibilityCheck(false, List.of("Complete your student profile to apply"));
        }

        List<String> reasons = new ArrayList<>();
        if (job.getMinCgpa() != null) {
            BigDecimal cgpa = toDecimal(snapshot.get("cgpa"));
            if (cgpa == null) {
                reasons.add("Add your CGPA to your profile");
            } else if (cgpa.compareTo(job.getMinCgpa()) < 0) {
                reasons.add("This role needs a CGPA of at least " + job.getMinCgpa());
            }
        }
        if (job.getMaxBacklogs() != null) {
            int backlogs = snapshot.get("backlogs") instanceof Number number ? number.intValue() : 0;
            if (backlogs > job.getMaxBacklogs()) {
                reasons.add("This role allows at most %d backlogs".formatted(job.getMaxBacklogs()));
            }
        }
        if (!job.getEligibleBatchIds().isEmpty()) {
            Long batchId = snapshot.get("batchId") instanceof Number number ? number.longValue() : null;
            if (batchId == null || !job.getEligibleBatchIds().contains(batchId)) {
                reasons.add("This drive is open to selected batches only");
            }
        }
        return reasons.isEmpty() ? EligibilityCheck.ok() : new EligibilityCheck(false, reasons);
    }

    @Transactional(readOnly = true)
    public Job load(Long jobId) {
        return jobs.findById(jobId).orElseThrow(() -> ApiException.notFound("Job", jobId));
    }

    /** Staff may edit any posting for their institution; an alumnus may edit only their own. */
    private void requireOwnership(Job job, AuthUser editor) {
        if (editor.isAdmin() || job.getPostedByUserId().equals(editor.id())) {
            return;
        }
        boolean sameInstitution = editor.isStaff() && job.getInstitutionId() != null
                && job.getInstitutionId().equals(editor.institutionId());
        if (!sameInstitution) {
            throw ApiException.forbidden("You cannot edit a posting you did not create");
        }
    }

    private void apply(Job job, JobRequest request) {
        job.setTitle(request.title());
        job.setDescription(request.description());
        job.setResponsibilities(request.responsibilities());
        job.setRequirements(request.requirements());
        job.setJobType(JobType.valueOf(request.jobType()));
        job.setWorkMode(request.workMode() == null ? WorkMode.ONSITE : WorkMode.valueOf(request.workMode()));
        job.setLocation(request.location());
        job.setSalaryMin(request.salaryMin());
        job.setSalaryMax(request.salaryMax());
        job.setMinExperienceMonths(request.minExperienceMonths());
        job.setMinCgpa(request.minCgpa());
        job.setMaxBacklogs(request.maxBacklogs());
        job.setEligibleBatchIds(new LinkedHashSet<>(request.eligibleBatchIds()));
        job.setRequiredSkills(new LinkedHashSet<>(request.requiredSkills()));
        job.setApplicationDeadline(request.applicationDeadline());
        job.setExpectedJoiningDate(request.expectedJoiningDate());
        job.setVacancies(request.vacancies());
        if (request.referralsEnabled() != null) {
            job.setReferralsEnabled(request.referralsEnabled());
        }
    }

    private void applyCompany(Company company, CompanyRequest request) {
        company.setDescription(request.description());
        company.setWebsite(request.website());
        company.setLogoUrl(request.logoUrl());
        company.setIndustry(request.industry());
        company.setHeadquarters(request.headquarters());
        company.setSizeBand(request.sizeBand());
        company.setContactName(request.contactName());
        company.setContactEmail(request.contactEmail());
        company.setContactPhone(request.contactPhone());
    }

    private Company loadCompany(Long companyId) {
        return companies.findById(companyId).orElseThrow(() -> ApiException.notFound("Company", companyId));
    }

    /** JSON gives us a number of some kind; a CGPA comparison needs a BigDecimal. */
    private static BigDecimal toDecimal(Object value) {
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        return value == null ? null : new BigDecimal(value.toString());
    }

    private static String like(String query) {
        return query == null || query.isBlank() ? null : "%" + query.trim().toLowerCase(Locale.ROOT) + "%";
    }

    /** Blank filters arrive as empty strings from the query string; both mean "no filter". */
    private static <E extends Enum<E>> E enumOrNull(Class<E> type, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw ApiException.badRequest("Unknown value: " + value);
        }
    }
}
