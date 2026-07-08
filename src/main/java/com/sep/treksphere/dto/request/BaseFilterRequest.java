package com.sep.treksphere.dto.request;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import io.swagger.v3.oas.annotations.media.Schema;

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
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        return PageRequest.of(page, size, sort);
    }
}
