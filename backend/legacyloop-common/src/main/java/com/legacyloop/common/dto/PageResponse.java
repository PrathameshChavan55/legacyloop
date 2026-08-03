package com.legacyloop.common.dto;

import java.util.List;
import java.util.function.Function;

/**
 * Transport-neutral page wrapper. Services convert a Spring {@code Page} into this so the
 * API contract does not leak Spring Data's serialisation shape (which changes between versions).
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last,
        boolean hasNext) {

    public static <T> PageResponse<T> of(List<T> content, int page, int size, long totalElements) {
        int totalPages = size == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        boolean last = page >= totalPages - 1;
        return new PageResponse<>(content, page, size, totalElements, totalPages,
                page == 0, last, !last);
    }

    public <R> PageResponse<R> map(Function<T, R> mapper) {
        return new PageResponse<>(content.stream().map(mapper).toList(),
                page, size, totalElements, totalPages, first, last, hasNext);
    }
}
