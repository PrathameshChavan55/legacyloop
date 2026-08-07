package com.legacyloop.career.controller;

import com.legacyloop.career.dto.AiDtos.AnalyseRequest;
import com.legacyloop.career.dto.AiDtos.AnalysisAccepted;
import com.legacyloop.career.dto.AiDtos.AnalysisResponse;
import com.legacyloop.career.dto.AiDtos.InterviewFeedbackRequest;
import com.legacyloop.career.dto.AiDtos.InterviewFeedbackResponse;
import com.legacyloop.career.dto.AiDtos.InterviewQuestionsRequest;
import com.legacyloop.career.dto.AiDtos.InterviewQuestionsResponse;
import com.legacyloop.career.dto.AiDtos.ResumeBuilderRequest;
import com.legacyloop.career.dto.AiDtos.ResumeBuilderResponse;
import com.legacyloop.career.service.AiService;
import com.legacyloop.common.ApiResponse;
import com.legacyloop.common.AuthUser;
import com.legacyloop.common.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Resume analysis, resume writing and interview practice. */
@Tag(name = "AI", description = "Resume analysis, resume builder and interview practice")
@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    @PostMapping("/resumes/{resumeId}/analyze")
    @Operation(summary = "Analyse a resume, optionally against a job; returns an id to poll")
    public ApiResponse<AnalysisAccepted> analyse(@AuthenticationPrincipal AuthUser user,
                                                 @PathVariable Long resumeId,
                                                 @RequestBody(required = false) AnalyseRequest request) {
        Long jobId = request == null ? null : request.jobId();
        return ApiResponse.ok(aiService.requestAnalysis(resumeId, jobId, user.id()));
    }

    @GetMapping("/analyses/{analysisId}")
    @Operation(summary = "One analysis, including its status")
    public ApiResponse<AnalysisResponse> analysis(@AuthenticationPrincipal AuthUser user,
                                                  @PathVariable Long analysisId) {
        return ApiResponse.ok(aiService.analysis(analysisId, user.id()));
    }

    @GetMapping("/resumes/{resumeId}/analysis")
    @Operation(summary = "The most recent analysis of one resume, or null")
    public ApiResponse<AnalysisResponse> latestForResume(@AuthenticationPrincipal AuthUser user,
                                                         @PathVariable Long resumeId) {
        return ApiResponse.ok(aiService.latestForResume(resumeId, user.id()));
    }

    @GetMapping("/analyses")
    @Operation(summary = "Your analysis history")
    public ApiResponse<PageResponse<AnalysisResponse>> history(@AuthenticationPrincipal AuthUser user,
                                                               @RequestParam(defaultValue = "0") int page,
                                                               @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(aiService.history(user.id(), PageRequest.of(page, Math.min(size, 50))));
    }

    @PostMapping("/resume-builder")
    @Operation(summary = "Draft a summary and bullet points for a target role")
    public ApiResponse<ResumeBuilderResponse> buildResume(@Valid @RequestBody ResumeBuilderRequest request) {
        return ApiResponse.ok(aiService.buildResume(request));
    }

    @PostMapping("/interview/questions")
    @Operation(summary = "Generate practice questions")
    public ApiResponse<InterviewQuestionsResponse> interviewQuestions(
            @Valid @RequestBody InterviewQuestionsRequest request) {
        return ApiResponse.ok(aiService.interviewQuestions(request));
    }

    @PostMapping("/interview/feedback")
    @Operation(summary = "Score a practice answer")
    public ApiResponse<InterviewFeedbackResponse> interviewFeedback(
            @Valid @RequestBody InterviewFeedbackRequest request) {
        return ApiResponse.ok(aiService.interviewFeedback(request));
    }
}
