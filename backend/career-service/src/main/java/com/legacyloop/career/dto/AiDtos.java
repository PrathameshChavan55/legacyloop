package com.legacyloop.career.dto;

import com.legacyloop.career.entity.ResumeAnalysis;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

public final class AiDtos {

    private AiDtos() {
    }

    public record AnalyseRequest(Long jobId) {
    }

    /** Returned immediately; the work happens in the background and the client polls the id. */
    public record AnalysisAccepted(Long analysisId, String status, String message) {
    }

    public record AnalysisResponse(Long id, Long resumeId, Long jobId, String jobTitle, String status,
                                   Integer score, String summary, List<String> strengths, List<String> gaps,
                                   List<String> suggestions, List<String> matchedSkills,
                                   List<String> missingSkills, String errorMessage, Instant requestedAt,
                                   Instant completedAt) {

        public static AnalysisResponse of(ResumeAnalysis analysis, JsonLists lists) {
            return new AnalysisResponse(analysis.getId(), analysis.getResumeId(), analysis.getJobId(),
                    analysis.getJobTitle(), analysis.getStatus().name(), analysis.getScore(),
                    analysis.getSummary(), lists.strengths(), lists.gaps(), lists.suggestions(),
                    lists.matchedSkills(), lists.missingSkills(), analysis.getErrorMessage(),
                    analysis.getRequestedAt(), analysis.getCompletedAt());
        }
    }

    /** The five JSON columns, already parsed, so the response factory stays a one-liner. */
    public record JsonLists(List<String> strengths, List<String> gaps, List<String> suggestions,
                            List<String> matchedSkills, List<String> missingSkills) {
    }

    public record ResumeBuilderRequest(
            @NotBlank @Size(max = 160) String targetRole,
            @Size(max = 2000) String experience,
            @Size(max = 1000) String skills,
            @Size(max = 2000) String education,
            @Size(max = 2000) String projects) {
    }

    public record ResumeBuilderResponse(String summary, List<String> bulletPoints, List<String> skillSuggestions) {
    }

    public record InterviewQuestionsRequest(
            @NotBlank @Size(max = 160) String role,
            @Size(max = 160) String company,
            @Size(max = 40) String difficulty,
            @Size(max = 1000) String focusAreas) {
    }

    public record InterviewQuestion(String question, String category, String hint) {
    }

    public record InterviewQuestionsResponse(String role, List<InterviewQuestion> questions) {
    }

    public record InterviewFeedbackRequest(
            @NotBlank @Size(max = 1000) String question,
            @NotBlank @Size(max = 5000) String answer) {
    }

    public record InterviewFeedbackResponse(int score, String verdict, List<String> strengths,
                                            List<String> improvements, String modelAnswer) {
    }
}
