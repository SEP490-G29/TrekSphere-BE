package com.sep.treksphere.matching.dto.request;

import com.sep.treksphere.common.dto.BaseFilterRequest;
import com.sep.treksphere.matching.enums.MatchingGroupSourceType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
public class MatchingGroupFilterRequest extends BaseFilterRequest {
    @Schema(description = "Loại nguồn nhóm: TOUR hoặc CUSTOM_JOURNEY (để trống để tìm tất cả)")
    private MatchingGroupSourceType sourceType;

    @Schema(description = "Lọc theo ID của Tour")
    private UUID tourId;

    @Schema(description = "Lọc theo ngày Trekker dự kiến đi cụ thể (yyyy-MM-dd)")
    private LocalDate targetDate;

    @Schema(description = "Lọc ngày dự kiến đi từ ngày (yyyy-MM-dd)")
    private LocalDate targetDateFrom;

    @Schema(description = "Lọc ngày dự kiến đi đến ngày (yyyy-MM-dd)")
    private LocalDate targetDateTo;

    @Schema(description = "Lọc theo độ khó hành trình (EASY, MODERATE, HARD, EXTREME/EXPERT)")
    private String difficulty;

    @Schema(description = "Lọc theo địa điểm / khu vực")
    private String location;

    @Schema(description = "Lọc theo chi phí tối thiểu")
    private BigDecimal minCost;

    @Schema(description = "Lọc theo chi phí tối đa")
    private BigDecimal maxCost;

    @Schema(description = "Chỉ lấy nhóm còn chỗ trống (mặc định true)")
    private Boolean availableSlotsOnly = true;
}
