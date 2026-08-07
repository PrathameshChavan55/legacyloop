package com.legacyloop.career.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Talks to Gemini, or says it cannot.
 *
 * <p>The original expressed this as a {@code GeminiClient} interface with a live implementation, a
 * stub implementation and a factory choosing between them on a Spring profile. The only question
 * that machinery answered was "is an API key configured?", so that is the question this class
 * asks directly — and callers handle {@link #isConfigured()} being false by falling back to a
 * rule-based result rather than failing.
 */
@Slf4j
@Component
public class GeminiClient {

    private static final String ENDPOINT =
            "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s";

    private final RestClient restClient = RestClient.create();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${legacyloop.gemini.api-key:}")
    private String apiKey;

    @Value("${legacyloop.gemini.model:gemini-1.5-flash}")
    private String model;

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    /**
     * Sends one prompt and returns the model's text.
     *
     * @return the response text, or null when the key is missing or the call fails — the caller
     *         then uses its own fallback, so a dead API never breaks a page.
     */
    public String generate(String prompt) {
        if (!isConfigured()) {
            return null;
        }
        try {
            Map<String, Object> body = Map.of("contents",
                    java.util.List.of(Map.of("parts", java.util.List.of(Map.of("text", prompt)))));

            String raw = restClient.post()
                    .uri(ENDPOINT.formatted(model, apiKey))
                    .body(body)
                    .retrieve()
                    .body(String.class);

            JsonNode text = objectMapper.readTree(raw)
                    .path("candidates").path(0).path("content").path("parts").path(0).path("text");
            return text.isMissingNode() ? null : text.asText();
        } catch (Exception ex) {
            log.warn("Gemini call failed: {}", ex.getMessage());
            return null;
        }
    }

    /** Strips the ```json fence models like to add, so the result can be parsed. */
    public String stripCodeFence(String text) {
        if (text == null) {
            return null;
        }
        String trimmed = text.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```(json)?", "").replaceFirst("```$", "");
        }
        return trimmed.trim();
    }
}

