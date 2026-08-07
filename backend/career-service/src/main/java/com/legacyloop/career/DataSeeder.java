package com.legacyloop.career;

import com.legacyloop.career.entity.Company;
import com.legacyloop.career.entity.Enums.JobStatus;
import com.legacyloop.career.entity.Enums.JobType;
import com.legacyloop.career.entity.Enums.WorkMode;
import com.legacyloop.career.entity.Job;
import com.legacyloop.career.repository.CompanyRepository;
import com.legacyloop.career.repository.JobRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Two employers and two live postings, so the board is not empty on first run.
 *
 * <p>The poster id matches the staff account user-service seeds. Set {@code SEED_DEMO_DATA=false}
 * to skip.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder {

    /** The staff account is the second user user-service creates. */
    private static final long DEMO_STAFF_USER_ID = 2L;
    private static final long DEMO_INSTITUTION_ID = 1L;

    private final CompanyRepository companies;
    private final JobRepository jobs;

    @Value("${legacyloop.seed-demo-data:true}")
    private boolean enabled;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seed() {
        if (!enabled || companies.count() > 0) {
            return;
        }
        log.info("Seeding demo companies and jobs");

        Company infotech = companies.save(Company.builder()
                .name("Infotech Labs")
                .description("Product engineering for banking and payments.")
                .website("https://infotechlabs.example")
                .industry("Software")
                .headquarters("Bengaluru")
                .sizeBand("501-1000")
                .contactName("Meera Iyer")
                .contactEmail("careers@infotechlabs.example")
                .verified(true)
                .build());

        Company northwind = companies.save(Company.builder()
                .name("Northwind Analytics")
                .description("Data platforms and machine learning consulting.")
                .website("https://northwind.example")
                .industry("Data")
                .headquarters("Pune")
                .sizeBand("51-200")
                .verified(true)
                .build());

        jobs.save(Job.builder()
                .title("Junior Backend Engineer")
                .description("Build and maintain Spring Boot services behind our payments platform. "
                        + "You will work with an experienced team, review code and ship weekly.")
                .responsibilities("Write REST APIs. Add tests. Take part in code review.")
                .requirements("Java fundamentals, SQL, an interest in distributed systems.")
                .company(infotech)
                .jobType(JobType.FULL_TIME)
                .workMode(WorkMode.HYBRID)
                .status(JobStatus.OPEN)
                .location("Bengaluru")
                .salaryMin(new BigDecimal("600000"))
                .salaryMax(new BigDecimal("900000"))
                .minCgpa(new BigDecimal("7.00"))
                .maxBacklogs(0)
                .requiredSkills(new LinkedHashSet<>(List.of("Java", "Spring Boot", "SQL")))
                .applicationDeadline(LocalDate.now().plusDays(30))
                .vacancies(4)
                .postedByUserId(DEMO_STAFF_USER_ID)
                .institutionId(DEMO_INSTITUTION_ID)
                .publishedAt(Instant.now())
                .build());

        jobs.save(Job.builder()
                .title("Data Engineering Intern")
                .description("Six-month internship building ingestion pipelines and dashboards.")
                .responsibilities("Write Python jobs. Model data. Present findings.")
                .requirements("Python, SQL, curiosity about data.")
                .company(northwind)
                .jobType(JobType.INTERNSHIP)
                .workMode(WorkMode.REMOTE)
                .status(JobStatus.OPEN)
                .location("Remote")
                .salaryMin(new BigDecimal("25000"))
                .requiredSkills(new LinkedHashSet<>(List.of("Python", "SQL")))
                .applicationDeadline(LocalDate.now().plusDays(45))
                .vacancies(2)
                .postedByUserId(DEMO_STAFF_USER_ID)
                .institutionId(DEMO_INSTITUTION_ID)
                .publishedAt(Instant.now())
                .build());
    }
}
