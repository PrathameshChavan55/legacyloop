package com.legacyloop.career.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.legacyloop.career.dto.AiDtos.AnalysisAccepted;
import com.legacyloop.career.dto.AiDtos.AnalysisResponse;
import com.legacyloop.career.dto.AiDtos.InterviewFeedbackRequest;
import com.legacyloop.career.dto.AiDtos.InterviewFeedbackResponse;
import com.legacyloop.career.dto.AiDtos.InterviewQuestion;
import com.legacyloop.career.dto.AiDtos.InterviewQuestionsRequest;
import com.legacyloop.career.dto.AiDtos.InterviewQuestionsResponse;
import com.legacyloop.career.dto.AiDtos.JsonLists;
import com.legacyloop.career.dto.AiDtos.ResumeBuilderRequest;
import com.legacyloop.career.dto.AiDtos.ResumeBuilderResponse;
import com.legacyloop.career.entity.Enums.AnalysisStatus;
import com.legacyloop.career.entity.Job;
import com.legacyloop.career.entity.ResumeAnalysis;
import com.legacyloop.career.repository.ResumeAnalysisRepository;
import com.legacyloop.common.ApiException;
import com.legacyloop.common.EventPublisher;
import com.legacyloop.common.Events;
import com.legacyloop.common.PageResponse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The AI features: resume analysis, a resume-writing helper and interview practice.
 *
 * <p>Analysis is asynchronous — the request returns an id straight away and the client polls it —
 * because a model call takes seconds and an HTTP request should not.
 *
 * <p>Every feature has a rule-based fallback used when no API key is configured. That is what
 * makes the project demonstrable offline, and it is why there is no separate "stub" class: the
 * fallback is the same method's else-branch.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiService {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final ResumeAnalysisRepository analyses;
    private final ResumeService resumeService;
    private final JobService jobService;
    private final GeminiClient gemini;
    private final EventPublisher events;

    /* ----------------------------------------------------------------- resume analysis */

    @Transactional
    public AnalysisAccepted requestAnalysis(Long resumeId, Long jobId, Long userId) {
        String resumeText = resumeService.textOf(resumeId, userId);
        Job job = jobId == null ? null : jobService.load(jobId);

        ResumeAnalysis analysis = analyses.save(ResumeAnalysis.builder()
                .resumeId(resumeId)
                .userId(userId)
                .jobId(jobId)
                .jobTitle(job == null ? null : job.getTitle())
                .build());

        runAnalysis(analysis.getId(), resumeText, job == null ? null : job.getDescription(),
                job == null ? Set.of() : job.getRequiredSkills());

        return new AnalysisAccepted(analysis.getId(), analysis.getStatus().name(),
                "We are reading your resume. This usually takes a few seconds.");
    }

    /**
     * Does the work and records the outcome on the row.
     *
     * <p>Called directly rather than through {@code @Async}: a method calling another method on
     * the same bean bypasses the Spring proxy, so the annotation would have been decoration
     * without effect. The client still polls the analysis id, because the row carries its own
     * status — which is what lets a failure surface as FAILED with a message instead of a request
     * that never returns.
     */
    @Transactional
    public void runAnalysis(Long analysisId, String resumeText, String jobDescription,
                            Set<String> requiredSkills) {
        ResumeAnalysis analysis = analyses.findById(analysisId).orElse(null);
        if (analysis == null) {
            return;
        }
        try {
            Findings findings = gemini.isConfigured()
                    ? askModel(resumeText, jobDescription, requiredSkills)
                    : null;
            if (findings == null) {
                findings = ruleBasedFindings(resumeText, requiredSkills);
            }

            analysis.setScore(findings.score());
            analysis.setSummary(findings.summary());
            analysis.setStrengthsJson(write(findings.strengths()));
            analysis.setGapsJson(write(findings.gaps()));
            analysis.setSuggestionsJson(write(findings.suggestions()));
            analysis.setMatchedSkillsJson(write(findings.matchedSkills()));
            analysis.setMissingSkillsJson(write(findings.missingSkills()));
            analysis.setStatus(AnalysisStatus.COMPLETED);
            analysis.setCompletedAt(Instant.now());

            events.publish(Events.RESUME_ANALYSED, analysis.getUserId(), "Resume analysis ready",
                    "Your resume scored %d out of 100.".formatted(findings.score()),
                    "/resume/analysis/" + analysis.getId());
        } catch (Exception ex) {
            log.error("Analysis {} failed", analysisId, ex);
            analysis.setStatus(AnalysisStatus.FAILED);
            analysis.setErrorMessage("We could not analyse that resume. Please try again.");
            analysis.setCompletedAt(Instant.now());
        }
    }

    @Transactional(readOnly = true)
    public AnalysisResponse analysis(Long analysisId, Long userId) {
        ResumeAnalysis analysis = analyses.findByIdAndUserId(analysisId, userId)
                .orElseThrow(() -> ApiException.notFound("Analysis", analysisId));
        return AnalysisResponse.of(analysis, readLists(analysis));
    }

    @Transactional(readOnly = true)
    public AnalysisResponse latestForResume(Long resumeId, Long userId) {
        return analyses.findFirstByResumeIdAndUserIdOrderByIdDesc(resumeId, userId)
                .map(analysis -> AnalysisResponse.of(analysis, readLists(analysis)))
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public PageResponse<AnalysisResponse> history(Long userId, Pageable pageable) {
        return PageResponse.of(analyses.findByUserIdOrderByIdDesc(userId, pageable),
                analysis -> AnalysisResponse.of(analysis, readLists(analysis)));
    }

    /* -------------------------------------------------------------- writing and practice */

    public ResumeBuilderResponse buildResume(ResumeBuilderRequest request) {
        String prompt = """
                Write resume content for a candidate applying for: %s.
                Experience: %s
                Skills: %s
                Education: %s
                Projects: %s

                Reply with JSON only, no markdown, in this shape:
                {"summary":"...","bulletPoints":["..."],"skillSuggestions":["..."]}
                """.formatted(request.targetRole(), nullToDash(request.experience()),
                nullToDash(request.skills()), nullToDash(request.education()),
                nullToDash(request.projects()));

        ResumeBuilderResponse fromModel = parse(gemini.generate(prompt), ResumeBuilderResponse.class);
        if (fromModel != null) {
            return fromModel;
        }

        // Fallback: assemble something useful from what the student typed.
        List<String> skills = splitWords(request.skills());
        return new ResumeBuilderResponse(
                "%s with hands-on experience in %s, looking for a %s role."
                        .formatted(request.targetRole(), skills.isEmpty() ? "software development"
                                : String.join(", ", skills.stream().limit(3).toList()),
                                request.targetRole()),
                List.of("Built and shipped projects using " + (skills.isEmpty() ? "modern tooling"
                                : String.join(", ", skills.stream().limit(4).toList())) + ".",
                        "Collaborated in a team using Git, code review and agile delivery.",
                        "Quantify each achievement: what you built, what changed, by how much."),
                List.of("Add a metric to every bullet point", "Name the tools you used, not the category",
                        "Keep the resume to one page"));
    }

    public InterviewQuestionsResponse interviewQuestions(InterviewQuestionsRequest request) {
        String prompt = """
                Generate 8 interview questions for a %s role%s at %s difficulty.
                Focus areas: %s.

                Reply with JSON only, no markdown:
                {"role":"...","questions":[{"question":"...","category":"...","hint":"..."}]}
                """.formatted(request.role(),
                request.company() == null || request.company().isBlank() ? ""
                        : " at " + request.company(),
                request.difficulty() == null ? "medium" : request.difficulty(),
                nullToDash(request.focusAreas()));

        InterviewQuestionsResponse fromModel = parse(gemini.generate(prompt), InterviewQuestionsResponse.class);
        if (fromModel != null) {
            return fromModel;
        }

        return new InterviewQuestionsResponse(request.role(), List.of(
                new InterviewQuestion("Walk me through a project you are proud of.", "Experience",
                        "Say what the problem was, what you chose, and why."),
                new InterviewQuestion("How would you design a URL shortener?", "System design",
                        "Start with the API, then storage, then scale."),
                new InterviewQuestion("What happens when you type a URL into a browser?", "Fundamentals",
                        "DNS, TCP, TLS, HTTP, rendering."),
                new InterviewQuestion("Explain the difference between a process and a thread.", "Operating systems",
                        "Talk about memory and scheduling."),
                new InterviewQuestion("How do you find the second-largest element in an array?", "Problem solving",
                        "One pass, two variables."),
                new InterviewQuestion("Describe a time you disagreed with a teammate.", "Behavioural",
                        "Situation, what you did, what came of it."),
                new InterviewQuestion("What is a database index and when does it hurt?", "Databases",
                        "Reads get faster, writes get slower."),
                new InterviewQuestion("Why do you want to work with us?", "Motivation",
                        "Say something only true of this company.")));
    }

    public InterviewFeedbackResponse interviewFeedback(InterviewFeedbackRequest request) {
        String prompt = """
                An interview candidate was asked: %s
                They answered: %s

                Score the answer out of 100 and reply with JSON only, no markdown:
                {"score":0,"verdict":"...","strengths":["..."],"improvements":["..."],"modelAnswer":"..."}
                """.formatted(request.question(), request.answer());

        InterviewFeedbackResponse fromModel = parse(gemini.generate(prompt), InterviewFeedbackResponse.class);
        if (fromModel != null) {
            return fromModel;
        }

        // Fallback: a length-and-structure heuristic, honest about being one.
        int words = request.answer().trim().split("\\s+").length;
        int score = Math.min(90, Math.max(35, words * 2));
        List<String> strengths = new ArrayList<>();
        List<String> improvements = new ArrayList<>();
        if (words > 60) {
            strengths.add("You gave a full answer rather than a one-liner");
        } else {
            improvements.add("Expand the answer — aim for a minute of speaking");
        }
        if (request.answer().toLowerCase(Locale.ROOT).matches(".*\\d.*")) {
            strengths.add("You used specifics rather than generalities");
        } else {
            improvements.add("Add a concrete number or example");
        }
        improvements.add("Structure it: situation, what you did, what happened");

        return new InterviewFeedbackResponse(score,
                score >= 70 ? "A solid answer" : "Worth another pass",
                strengths, improvements,
                "Set the scene in one sentence, spend most of the answer on what you did and why, "
                        + "and finish with the outcome.");
    }

    /* -------------------------------------------------------------------------- internals */

    /** What an analysis produces, whichever way it was produced. */
    private record Findings(int score, String summary, List<String> strengths, List<String> gaps,
                            List<String> suggestions, List<String> matchedSkills,
                            List<String> missingSkills) {
    }

    private Findings askModel(String resumeText, String jobDescription, Set<String> requiredSkills) {
        String prompt = """
                Review this resume%s.

                Resume:
                %s
                %s

                Reply with JSON only, no markdown, in this shape:
                {"score":0,"summary":"...","strengths":["..."],"gaps":["..."],"suggestions":["..."],
                 "matchedSkills":["..."],"missingSkills":["..."]}
                """.formatted(
                jobDescription == null ? "" : " against the job description below",
                truncate(resumeText, 12_000),
                jobDescription == null ? "" : "\nJob description:\n" + truncate(jobDescription, 4_000));

        Findings findings = parse(gemini.generate(prompt), Findings.class);
        if (findings == null) {
            return null;
        }
        // The model is not trusted with the skill match when we already know the required list.
        return requiredSkills.isEmpty() ? findings
                : new Findings(findings.score(), findings.summary(), findings.strengths(), findings.gaps(),
                        findings.suggestions(), matched(resumeText, requiredSkills),
                        missing(resumeText, requiredSkills));
    }

    /**
     * The offline analysis: match the required skills against the resume text and score on
     * coverage plus the sections a resume ought to have.
     */
    private Findings ruleBasedFindings(String resumeText, Set<String> requiredSkills) {
        String lower = resumeText.toLowerCase(Locale.ROOT);
        List<String> matched = matched(resumeText, requiredSkills);
        List<String> missing = missing(resumeText, requiredSkills);

        List<String> strengths = new ArrayList<>();
        List<String> gaps = new ArrayList<>();
        List<String> suggestions = new ArrayList<>();

        for (String section : List.of("experience", "education", "project", "skill")) {
            if (lower.contains(section)) {
                strengths.add("Has a " + section + " section");
            } else {
                gaps.add("No " + section + " section found");
            }
        }
        if (lower.matches("(?s).*\\d+%.*")) {
            strengths.add("Uses measurable results");
        } else {
            suggestions.add("Quantify your achievements — percentages, users, time saved");
        }
        if (!missing.isEmpty()) {
            suggestions.add("The job asks for " + String.join(", ", missing)
                    + " — mention these if you have used them");
        }
        suggestions.add("Keep it to one page and lead each bullet with a verb");

        int coverage = requiredSkills.isEmpty() ? 70
                : matched.size() * 100 / requiredSkills.size();
        int score = Math.min(95, (coverage + strengths.size() * 10) / 2 + 20);

        return new Findings(score,
                requiredSkills.isEmpty()
                        ? "A structural review of your resume."
                        : "Your resume covers %d of %d skills this role asks for."
                                .formatted(matched.size(), requiredSkills.size()),
                strengths, gaps, suggestions, matched, missing);
    }

    private static List<String> matched(String resumeText, Set<String> requiredSkills) {
        String lower = resumeText.toLowerCase(Locale.ROOT);
        return requiredSkills.stream().filter(skill -> lower.contains(skill.toLowerCase(Locale.ROOT))).toList();
    }

    private static List<String> missing(String resumeText, Set<String> requiredSkills) {
        String lower = resumeText.toLowerCase(Locale.ROOT);
        return requiredSkills.stream().filter(skill -> !lower.contains(skill.toLowerCase(Locale.ROOT))).toList();
    }

    private JsonLists readLists(ResumeAnalysis analysis) {
        return new JsonLists(read(analysis.getStrengthsJson()), read(analysis.getGapsJson()),
                read(analysis.getSuggestionsJson()), read(analysis.getMatchedSkillsJson()),
                read(analysis.getMissingSkillsJson()));
    }

    private List<String> read(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return JSON.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception ex) {
            return List.of();
        }
    }

    private String write(List<String> values) {
        try {
            return JSON.writeValueAsString(values == null ? List.of() : values);
        } catch (Exception ex) {
            return "[]";
        }
    }

    /** @return the parsed object, or null when the model said nothing usable. */
    private <T> T parse(String modelOutput, Class<T> type) {
        String cleaned = gemini.stripCodeFence(modelOutput);
        if (cleaned == null || cleaned.isBlank()) {
            return null;
        }
        try {
            return JSON.readValue(cleaned, type);
        } catch (Exception ex) {
            log.warn("Could not parse the model's reply as {}: {}", type.getSimpleName(), ex.getMessage());
            return null;
        }
    }

    private static List<String> splitWords(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return new ArrayList<>(new LinkedHashSet<>(Arrays.stream(value.split("[,;\n]"))
                .map(String::trim).filter(word -> !word.isEmpty()).toList()));
    }

    private static String truncate(String value, int limit) {
        return value.length() <= limit ? value : value.substring(0, limit);
    }

    private static String nullToDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}

