package com.dsi.support.agenticrouter.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Objects;

public final class PaginationUtils {

    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;

    private PaginationUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    public static Pageable normalize(
        int page,
        int size
    ) {
        return normalize(
            page,
            size,
            Sort.unsorted()
        );
    }

    public static Pageable normalize(
        int page,
        int size,
        Sort sort
    ) {
        int normalizedPage = Math.max(page, 0);
        int normalizedSize = size <= 0
            ? DEFAULT_PAGE_SIZE
            : Math.min(size, MAX_PAGE_SIZE);

        Sort normalizedSort = Objects.isNull(sort)
            ? Sort.unsorted()
            : sort;

        return PageRequest.of(
            normalizedPage,
            normalizedSize,
            normalizedSort
        );
    }

    public static Pageable normalize(
        Pageable pageable
    ) {
        return normalize(
            pageable,
            Sort.unsorted()
        );
    }

    public static Pageable normalize(
        Pageable pageable,
        Sort defaultSort
    ) {
        if (Objects.isNull(pageable)) {
            return normalize(
                0,
                DEFAULT_PAGE_SIZE,
                defaultSort
            );
        }

        Sort normalizedSort = pageable.getSort().isSorted()
            ? pageable.getSort()
            : Objects.requireNonNullElse(defaultSort, Sort.unsorted());

        return normalize(
            pageable.getPageNumber(),
            pageable.getPageSize(),
            normalizedSort
        );
    }
}
