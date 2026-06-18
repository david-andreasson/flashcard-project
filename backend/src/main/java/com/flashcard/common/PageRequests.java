package com.flashcard.common;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Builds {@link Pageable}s from request params with a default size and a hard cap, so a client
 * cannot request an unbounded page.
 */
public final class PageRequests {

    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    private PageRequests() {
    }

    public static Pageable of(Integer page, Integer size, Sort sort) {
        int p = (page == null || page < 0) ? 0 : page;
        int s = (size == null || size < 1) ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        return PageRequest.of(p, s, sort);
    }

    public static Pageable of(Integer page, Integer size) {
        return of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
    }
}
