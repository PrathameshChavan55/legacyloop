package com.legacyloop.career.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Reads people from user-service.
 *
 * <p>Everything here is a plain HTTP call with a fallback, not a Feign client with a circuit
 * breaker and a fallback factory: three call sites do not need that machinery, and a missing name
 * should degrade to "Unknown" rather than fail the page.
 *
 * <p>{@link #names} takes a list on purpose. Rendering fifty applications must be one call, not
 * fifty.
 */
@Slf4j
@Component
public class UserClient {

    /** Mirrors user-service's UserSummary; only the fields this service reads. */
    public record UserSummary(Long id, String fullName, String email, List<String> roles,
                              Long institutionId, String studentIdentifier, boolean premium, boolean active) {

        public static UserSummary unknown(Long id) {
            return new UserSummary(id, "Unknown user", null, List.of(), null, null, false, false);
        }
    }

    private final RestClient restClient;

    public UserClient(RestClient.Builder builder, @Value("${legacyloop.user-service-url}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    public UserSummary user(Long userId) {
        return names(List.of(userId)).getOrDefault(userId, UserSummary.unknown(userId));
    }

    public String name(Long userId) {
        return user(userId).fullName();
    }

    /** @return id to summary; ids that could not be resolved are simply absent. */
    public Map<Long, UserSummary> names(List<Long> userIds) {
        List<Long> ids = userIds.stream().filter(java.util.Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        try {
            var response = restClient.post()
                    .uri("/internal/v1/users/bulk")
                    .body(ids)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Envelope<Map<Long, UserSummary>>>() {});
            return response == null || response.data() == null ? Map.of() : response.data();
        } catch (Exception ex) {
            log.warn("Could not load users {}: {}", ids, ex.getMessage());
            return Map.of();
        }
    }

    /** CGPA, backlogs and batch, used to decide whether a student may apply. */
    public Map<String, Object> eligibilitySnapshot(Long userId) {
        try {
            var response = restClient.get()
                    .uri("/internal/v1/profiles/{userId}/eligibility", userId)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Envelope<Map<String, Object>>>() {});
            return response == null || response.data() == null ? Map.of() : response.data();
        } catch (Exception ex) {
            log.warn("Could not load the eligibility snapshot for {}: {}", userId, ex.getMessage());
            return Map.of();
        }
    }

    public List<Map<String, Object>> referrersAt(String company) {
        try {
            var response = restClient.get()
                    .uri(builder -> builder.path("/internal/v1/profiles/referrers")
                            .queryParam("company", company).build())
                    .retrieve()
                    .body(new ParameterizedTypeReference<Envelope<List<Map<String, Object>>>>() {});
            return response == null || response.data() == null ? List.of() : response.data();
        } catch (Exception ex) {
            log.warn("Could not load referrers at {}: {}", company, ex.getMessage());
            return List.of();
        }
    }

    /** Placed-student and mentor counts, which live with the profiles rather than the jobs. */
    public Map<String, Long> placementStats() {
        try {
            var response = restClient.get()
                    .uri("/internal/v1/profiles/stats")
                    .retrieve()
                    .body(new ParameterizedTypeReference<Envelope<Map<String, Long>>>() {});
            return response == null || response.data() == null ? Map.of() : response.data();
        } catch (Exception ex) {
            log.warn("Could not load placement stats: {}", ex.getMessage());
            return Map.of();
        }
    }

    /** Tells user-service to mark the student placed once an offer is recorded. */
    public void markPlaced(Long userId, String company, BigDecimal packageValue) {
        try {
            restClient.post()
                    .uri(builder -> builder.path("/internal/v1/profiles/{userId}/placed")
                            .queryParam("company", company)
                            .queryParam("packageValue", packageValue)
                            .build(userId))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception ex) {
            log.warn("Could not mark user {} as placed: {}", userId, ex.getMessage());
        }
    }

    /** The response envelope every service returns; only {@code data} matters here. */
    private record Envelope<T>(boolean success, String message, T data) {
    }
}
