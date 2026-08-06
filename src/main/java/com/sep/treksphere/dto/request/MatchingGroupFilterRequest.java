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

    @Schema(description = "Lọc theo ngày Trekker dự kiến đi, không phụ thuộc lịch khởi hành của Tour (yyyy-MM-dd)")
    private LocalDate targetDate;
}
