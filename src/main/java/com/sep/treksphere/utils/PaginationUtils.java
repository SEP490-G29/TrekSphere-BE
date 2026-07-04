package com.sep.treksphere.utils;

import com.sep.treksphere.dto.response.PaginationResponse;
import org.springframework.data.domain.Page;

public class PaginationUtils {

    private PaginationUtils() {
    }

    public static <T> PaginationResponse<T> toPaginationResponse(Page<T> page) {
        return PaginationResponse.<T>builder()
                .content(page.getContent())
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }
}
