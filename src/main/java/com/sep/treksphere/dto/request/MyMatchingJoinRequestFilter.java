package com.sep.treksphere.dto.request;

import com.sep.treksphere.constant.MessageConstant;
import com.sep.treksphere.enums.matching.JoinStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Getter
@Setter
public class MyMatchingJoinRequestFilter {

    private JoinStatus status;

    @Min(value = 0, message = MessageConstant.MATCHING_JOIN_REQUEST_PAGE_MIN)
    @Schema(defaultValue = "0", minimum = "0")
    private int page = 0;

    @Min(value = 1, message = MessageConstant.MATCHING_JOIN_REQUEST_SIZE_RANGE)
    @Max(value = 50, message = MessageConstant.MATCHING_JOIN_REQUEST_SIZE_RANGE)
    @Schema(defaultValue = "10", minimum = "1", maximum = "50")
    private int size = 10;

    public Pageable getPageable() {
        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
    }
}
