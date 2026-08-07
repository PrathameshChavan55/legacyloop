package com.legacyloop.social.service;

import com.legacyloop.social.dto.SocialDtos.Author;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Resolves ids to names, by asking user-service.
 *
 * <p>Deliberately bulk-first: a feed page holds twenty posts by up to twenty people, and the whole
 * page must cost one call. {@link #author} exists for the single-post case and goes through the
 * same method.
 */
@Slf4j
@Component
public class PeopleClient {

    private record UserSummary(Long id, String fullName, String email) {
    }

    private record Envelope<T>(boolean success, String message, T data) {
    }

    private final RestClient restClient;

    public PeopleClient(RestClient.Builder builder, @Value("${legacyloop.user-service-url}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    public Map<Long, Author> authors(List<Long> userIds) {
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

            if (response == null || response.data() == null) {
                return Map.of();
            }
            return response.data().values().stream()
                    .collect(Collectors.toMap(UserSummary::id,
                            summary -> new Author(summary.id(), summary.fullName(), null, null)));
        } catch (Exception ex) {
            log.warn("Could not resolve users {}: {}", ids, ex.getMessage());
            return Map.of();
        }
    }

    public Author author(Long userId) {
        return authors(List.of(userId)).getOrDefault(userId, unknown(userId));
    }

    /** A missing name should leave the page readable, not blank it. */
    public static Author unknown(Long userId) {
        return new Author(userId, "LegacyLoop member", null, null);
    }

    public Author authorOrUnknown(Map<Long, Author> resolved, Long userId) {
        return resolved.getOrDefault(userId, unknown(userId));
    }
}
