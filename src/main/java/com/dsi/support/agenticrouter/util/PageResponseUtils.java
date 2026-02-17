package com.dsi.support.agenticrouter.util;

import com.dsi.support.agenticrouter.dto.api.ApiDtos;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

public final class PageResponseUtils {

    private PageResponseUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    public static <T, R> ApiDtos.PagedResponse<R> fromPage(
        Page<T> page,
        Function<T, R> mapper
    ) {
        List<R> content = page.getContent()
                              .stream()
                              .map(mapper)
                              .toList();

        return fromPage(
            page,
            content
        );
    }

    public static <T> ApiDtos.PagedResponse<T> fromPage(
        Page<?> page,
        List<T> content
    ) {
        return ApiDtos.PagedResponse.<T>builder()
                                    .content(content)
                                    .page(page.getNumber())
                                    .size(page.getSize())
                                    .totalElements(page.getTotalElements())
                                    .totalPages(page.getTotalPages())
                                    .hasNext(page.hasNext())
                                    .build();
    }
}
