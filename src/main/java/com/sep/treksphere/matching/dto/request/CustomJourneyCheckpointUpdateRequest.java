package com.sep.treksphere.matching.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request cập nhật điểm dừng checkpoint trong hành trình Custom Journey")
public class CustomJourneyCheckpointUpdateRequest {

    @Min(value = 1, message = "Số ngày (dayNo) phải lớn hơn hoặc bằng 1")
    @Schema(description = "Ngày thứ mấy trong hành trình", example = "1")
    private Integer dayNo;

    @Min(value = 1, message = "Thứ tự điểm dừng phải lớn hơn hoặc bằng 1")
    @Schema(description = "Thứ tự điểm dừng/hoạt động", example = "1")
    private Integer checkpointOrder;

    @Size(max = 200, message = "Tiêu đề điểm dừng không được vượt quá 200 ký tự")
    @Schema(description = "Tiêu đề điểm dừng/hoạt động", example = "Tập trung tại chân núi và bắt đầu leo")
    private String title;

    @Schema(description = "Mô tả chi tiết điểm dừng/hoạt động", example = "Kiểm tra trang thiết bị và khởi hành chặng 1")
    private String description;

    @Size(max = 200, message = "Tên địa điểm không được vượt quá 200 ký tự")
    @Schema(description = "Tên địa điểm hiển thị", example = "Trạm kiểm lâm Lảo Thẩn")
    private String locationName;

    @Schema(description = "Vĩ độ địa lý", example = "22.6123456")
    private BigDecimal latitude;

    @Schema(description = "Kinh độ địa lý", example = "103.6123456")
    private BigDecimal longitude;

    @Schema(description = "Thời gian bắt đầu dự kiến", example = "2026-10-15T08:00:00")
    private LocalDateTime plannedStartAt;

    @Schema(description = "Thời gian kết thúc dự kiến", example = "2026-10-15T11:30:00")
    private LocalDateTime plannedEndAt;

    @Size(max = 500, message = "Đường dẫn ảnh không được vượt quá 500 ký tự")
    @Schema(description = "Ảnh minh hoạ điểm dừng", example = "https://example.com/checkpoint1.jpg")
    private String imageUrl;
}
