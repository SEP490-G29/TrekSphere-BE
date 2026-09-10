package com.sep.treksphere.matching.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sep.treksphere.matching.enums.TimeSlot;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request tạo hoạt động trong thời khóa biểu hành trình")
public class CustomJourneyActivityCreateRequest {

    @JsonAlias({"checkpointId", "customJourneyCheckpointId"})
    @JsonProperty("customJourneyCheckpointId")
    @Schema(description = "ID điểm dừng/checkpoint liên kết (nếu có)", example = "45597528-bff9-4605-b31b-23e8dd6abf5f")
    private UUID customJourneyCheckpointId;

    @NotNull(message = "Số ngày (dayNo) không được để trống")
    @Min(value = 1, message = "Số ngày (dayNo) phải lớn hơn hoặc bằng 1")
    @Schema(description = "Ngày thứ mấy trong hành trình", example = "1")
    private Integer dayNo;

    @NotNull(message = "Buổi trong ngày (timeSlot) không được để trống")
    @Schema(description = "Buổi trong ngày (MORNING, NOON, AFTERNOON, EVENING)", example = "MORNING")
    private TimeSlot timeSlot;

    @NotNull(message = "Thứ tự hoạt động không được để trống")
    @Min(value = 1, message = "Thứ tự hoạt động phải lớn hơn hoặc bằng 1")
    @Schema(description = "Thứ tự hoạt động trong buổi", example = "1")
    private Integer activityOrder;

    @NotBlank(message = "Tiêu đề hoạt động không được để trống")
    @Size(max = 255, message = "Tiêu đề hoạt động không được vượt quá 255 ký tự")
    @Schema(description = "Tiêu đề hoạt động", example = "Ăn sáng và kiểm tra ba lô")
    private String title;

    @Schema(description = "Mô tả chi tiết hoạt động", example = "Tập trung tại sảnh homestay, ăn sáng và chuẩn bị nước uống")
    private String description;

    @Size(max = 50, message = "Thời gian bắt đầu không được vượt quá 50 ký tự")
    @Schema(description = "Thời gian bắt đầu dự kiến (VD: 07:30 hoặc 2026-10-15T07:30:00)", example = "07:30")
    private String plannedStartAt;

    @Size(max = 50, message = "Thời gian kết thúc không được vượt quá 50 ký tự")
    @Schema(description = "Thời gian kết thúc dự kiến (VD: 08:30 hoặc 2026-10-15T08:30:00)", example = "08:30")
    private String plannedEndAt;
}
