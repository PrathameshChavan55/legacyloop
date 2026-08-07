package com.legacyloop.user;

import com.legacyloop.common.Roles;
import com.legacyloop.user.entity.AcademicUnit;
import com.legacyloop.user.entity.AcademicUnit.Type;
import com.legacyloop.user.entity.AlumniProfile;
import com.legacyloop.user.entity.Institution;
import com.legacyloop.user.entity.Plan;
import com.legacyloop.user.entity.Skill;
import com.legacyloop.user.entity.StudentProfile;
import com.legacyloop.user.entity.User;
import com.legacyloop.user.entity.UserStatus;
import com.legacyloop.user.repository.AcademicUnitRepository;
import com.legacyloop.user.repository.AlumniProfileRepository;
import com.legacyloop.user.repository.InstitutionRepository;
import com.legacyloop.user.repository.PlanRepository;
import com.legacyloop.user.repository.SkillRepository;
import com.legacyloop.user.repository.StudentProfileRepository;
import com.legacyloop.user.repository.UserRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds enough data to sign in and demonstrate every screen.
 *
 * <p>Everything here is idempotent — it checks before it inserts — so restarting the service is
 * safe. Set {@code SEED_DEMO_DATA=false} to skip it.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder {

    private static final String DEMO_PASSWORD = "Passw0rd!";

    private final InstitutionRepository institutions;
    private final AcademicUnitRepository academics;
    private final UserRepository users;
    private final StudentProfileRepository studentProfiles;
    private final AlumniProfileRepository alumniProfiles;
    private final PlanRepository plans;
    private final SkillRepository skills;
    private final PasswordEncoder passwordEncoder;

    @Value("${legacyloop.seed-demo-data:true}")
    private boolean enabled;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seed() {
        if (!enabled || institutions.count() > 0) {
            return;
        }
        log.info("Seeding demo data");

        Institution institution = institutions.save(Institution.builder()
                .code("DEMO")
                .name("Demo Institute of Technology")
                .shortName("DIT")
                .primaryColor("#4f46e5")
                .identifierLabel("Roll number")
                .identifierPattern("^[A-Z]{2}\\d{6}$")
                .staffRoleLabel("Placement Head")
                .contactEmail("placement@demo.edu")
                .city("Pune")
                .build());

        AcademicUnit department = academics.save(unit(institution.getId(), Type.DEPARTMENT,
                "CS", "Computer Science", null));
        AcademicUnit program = academics.save(unit(institution.getId(), Type.PROGRAM,
                "PGDAC", "PG Diploma in Advanced Computing", department.getId()));
        academics.save(unit(institution.getId(), Type.BRANCH, "SE", "Software Engineering", program.getId()));

        AcademicUnit batch = unit(institution.getId(), Type.BATCH, "AUG2026", "August 2026", program.getId());
        batch.setStartYear(2026);
        batch.setEndYear(2027);
        batch.setPlacementOpen(true);
        batch = academics.save(batch);

        User admin = createUser(institution.getId(), "admin@legacyloop.local", "Platform", "Admin",
                Roles.ROLE_ADMIN, null);
        createUser(institution.getId(), "staff@legacyloop.local", "Priya", "Nair", Roles.ROLE_STAFF, null);
        User student = createUser(institution.getId(), "student@legacyloop.local", "Rahul", "Deshmukh",
                Roles.ROLE_STUDENT, "DT202601");
        User alumnus = createUser(institution.getId(), "alumni@legacyloop.local", "Anita", "Rao",
                Roles.ROLE_ALUMNI, null);

        studentProfiles.save(StudentProfile.builder()
                .userId(student.getId())
                .institutionId(institution.getId())
                .departmentId(department.getId())
                .programId(program.getId())
                .batchId(batch.getId())
                .cgpa(new BigDecimal("8.40"))
                .graduationYear(2027)
                .headline("Full-stack developer in the making")
                .about("Building web applications with Spring Boot and React.")
                .location("Pune")
                .skills(new java.util.LinkedHashSet<>(List.of("Java", "Spring Boot", "React", "SQL")))
                .build());

        alumniProfiles.save(AlumniProfile.builder()
                .userId(alumnus.getId())
                .institutionId(institution.getId())
                .programId(program.getId())
                .graduationYear(2020)
                .currentCompany("Infotech Labs")
                .currentDesignation("Senior Software Engineer")
                .currentLocation("Bengaluru")
                .industry("Software")
                .totalExperienceMonths(62)
                .headline("Backend engineer, happy to refer")
                .skills(new java.util.LinkedHashSet<>(List.of("Java", "Microservices", "AWS")))
                .willingToRefer(true)
                .availableForMentorship(true)
                .mentorshipAreas("System design, interview preparation")
                .build());

        plans.saveAll(List.of(
                Plan.builder().code("MONTHLY").name("Premium Monthly")
                        .description("Everything premium, billed monthly")
                        .amountPaise(49_900L).durationDays(30).displayOrder(1)
                        .features(new java.util.ArrayList<>(List.of("Unlimited AI resume analyses",
                                "Priority in referral requests", "Interview question generator")))
                        .build(),
                Plan.builder().code("YEARLY").name("Premium Yearly")
                        .description("Two months free compared with monthly")
                        .amountPaise(499_900L).durationDays(365).recommended(true).displayOrder(2)
                        .features(new java.util.ArrayList<>(List.of("Everything in monthly",
                                "AI resume builder", "Profile highlighted to recruiters")))
                        .build()));

        List.of("Java", "Spring Boot", "React", "SQL", "Python", "Microservices", "AWS", "Docker")
                .forEach(name -> skills.save(Skill.builder().name(name).approved(true).usageCount(1).build()));

        log.info("Demo data ready. Sign in as {} with password {}", admin.getEmail(), DEMO_PASSWORD);
    }

    private AcademicUnit unit(Long institutionId, Type type, String code, String name, Long parentId) {
        return AcademicUnit.builder()
                .institutionId(institutionId).type(type).code(code).name(name).parentId(parentId).build();
    }

    private User createUser(Long institutionId, String email, String firstName, String lastName,
                            String role, String identifier) {
        return users.save(User.builder()
                .email(email)
                .password(passwordEncoder.encode(DEMO_PASSWORD))
                .firstName(firstName)
                .lastName(lastName)
                .status(UserStatus.ACTIVE)
                .roles(Set.of(role))
                .institutionId(institutionId)
                .studentIdentifier(identifier)
                .emailVerified(true)
                .build());
    }
}
