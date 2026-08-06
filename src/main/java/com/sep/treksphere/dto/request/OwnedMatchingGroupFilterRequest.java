package com.sep.treksphere.dto.request;

import com.sep.treksphere.enums.matching.MatchingGroupStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.util.StringUtils;

import java.util.Set;

@Getter
@Setter
public class OwnedMatchingGroupFilterRequest extends BaseFilterRequest {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "createdAt",
            "groupName",
            "targetDate",
            "matchingDeadline",
            "status"
    );

    @Schema(description = "Lọc theo trạng thái nhóm do Trekker quản lý")
    private MatchingGroupStatus status;

    @Override
    public Pageable getPageable() {
        String requestedSortBy = StringUtils.hasText(getSortBy()) ? getSortBy().trim() : "createdAt";
        String validSortBy = ALLOWED_SORT_FIELDS.contains(requestedSortBy) ? requestedSortBy : "createdAt";
        Sort.Direction direction = "asc".equalsIgnoreCase(getSortDir())
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        int validPage = Math.max(getPage(), 0);
        int validSize = getSize() <= 0 ? 10 : Math.min(getSize(), MAX_PAGE_SIZE);

        return PageRequest.of(validPage, validSize, Sort.by(direction, validSortBy));
    }
}
