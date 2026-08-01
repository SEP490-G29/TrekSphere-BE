package com.sep.treksphere.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import org.springframework.util.StringUtils;

@Getter
@Setter
public class BaseFilterRequest {
    @Schema(description = "Từ khóa tìm kiếm")
    private String keyword;

    @Schema(defaultValue = "0")
    private int page = 0;

    @Schema(defaultValue = "10")
    private int size = 10;

    @Schema(defaultValue = "createdAt")
    private String sortBy = "createdAt";

    @Schema(defaultValue = "desc")
    private String sortDir = "desc";

    public Pageable getPageable() {
        String validSortBy = StringUtils.hasText(sortBy) ? sortBy.trim() : "createdAt";
        String validSortDir = StringUtils.hasText(sortDir) ? sortDir.trim() : "desc";

        Sort sort = validSortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(validSortBy).ascending()
                : Sort.by(validSortBy).descending();

        int validPage = page < 0 ? 0 : page;
        int validSize = size <= 0 ? 10 : size;

        return PageRequest.of(validPage, validSize, sort);
    }
}
