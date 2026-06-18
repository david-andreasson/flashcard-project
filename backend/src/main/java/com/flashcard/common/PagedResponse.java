package com.flashcard.common;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Stable pagination envelope returned by all list endpoints. Avoids serializing Spring Data's
 * {@code PageImpl}, whose JSON shape is explicitly unstable across versions.
 */
public record PagedResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static <T> PagedResponse<T> from(Page<T> page) {
        return new PagedResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
