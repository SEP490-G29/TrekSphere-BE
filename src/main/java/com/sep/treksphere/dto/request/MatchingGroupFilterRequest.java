package com.sep.treksphere.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
public class MatchingGroupFilterRequest extends BaseFilterRequest {
    @Schema(description = "Lọc theo ID của Tour")
    private UUID tourId;

    @Schema(description = "Lọc theo ngày khởi hành dự kiến (yyyy-MM-dd)")
    private LocalDate targetDate;
}
