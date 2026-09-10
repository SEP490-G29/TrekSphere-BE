package com.sep.treksphere.matching.dto.request;

import com.sep.treksphere.common.constant.MessageConstant;
import com.sep.treksphere.matching.enums.JourneyDifficulty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Request cập nhật thông tin hành trình Custom Journey")
public class CustomJourneyUpdateRequest {

    @Size(max = 200, message = MessageConstant.CUSTOM_JOURNEY_TITLE_MAX_LENGTH)
    @Schema(description = "Tiêu đề hành trình", example = "Chinh phục Lảo Thẩn săn mây 2N1Đ - Bản cập nhật")
    private String title;

    @Size(max = 2000, message = MessageConstant.CUSTOM_JOURNEY_DESCRIPTION_MAX_LENGTH)
    @Schema(description = "Mô tả chi tiết hành trình", example = "Lịch trình chi tiết cập nhật thêm điểm ngắm bình minh")
    private String description;

    @Schema(description = "Độ khó hành trình (EASY, MODERATE, HARD, EXTREME)", example = "MODERATE")
    private JourneyDifficulty difficulty;

    @Schema(description = "Ngày bắt đầu hành trình", example = "2026-10-15")
    private LocalDate startDate;

    @Schema(description = "Ngày kết thúc hành trình", example = "2026-10-16")
    private LocalDate endDate;
}
