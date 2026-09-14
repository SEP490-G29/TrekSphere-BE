package com.sep.treksphere.matching.dto.request;

import com.sep.treksphere.common.constant.MessageConstant;
import com.sep.treksphere.matching.enums.MatchingGroupSourceType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Request tạo nhóm ghép bạn đồng hành")
public class MatchingGroupCreateRequest {

    @NotNull(message = MessageConstant.MATCHING_GROUP_SOURCE_TYPE_REQUIRED)
    @Schema(description = "Loại nguồn chuyến đi (TOUR hoặc CUSTOM_JOURNEY)", example = "CUSTOM_JOURNEY")
    private MatchingGroupSourceType sourceType;

    @Schema(description = "Mã tour (nếu sourceType là TOUR)", example = "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11")
    private UUID tourId;

    @Valid
    @Schema(description = "Thông tin hành trình tùy chỉnh (nếu sourceType là CUSTOM_JOURNEY)")
    private CustomJourneyCreateRequest customJourney;

    @NotBlank(message = MessageConstant.MATCHING_GROUP_NAME_REQUIRED)
    @Size(min = 3, max = 100, message = MessageConstant.MATCHING_GROUP_NAME_SIZE)
    @Schema(description = "Tên nhóm ghép", example = "Săn mây Tà Xùa cuối tuần")
    private String groupName;

    @Size(max = 2000, message = MessageConstant.MATCHING_GROUP_DESCRIPTION_MAX_LENGTH)
    @Schema(description = "Mô tả chi tiết nhóm ghép", example = "Tìm 4 bạn đam mê trekking săn mây cùng chinh phục đỉnh Tà Xùa")
    private String description;

    @Size(max = 500, message = MessageConstant.MATCHING_GROUP_COVER_IMAGE_MAX_LENGTH)
    @Schema(description = "Đường dẫn ảnh bìa của nhóm", example = "https://res.cloudinary.com/demo/image/upload/sample.jpg")
    private String coverImageUrl;

    @NotNull(message = MessageConstant.MATCHING_GROUP_MAX_SIZE_REQUIRED)
    @Min(value = 2, message = MessageConstant.MATCHING_GROUP_MAX_SIZE_MIN)
    @Max(value = 100, message = MessageConstant.MATCHING_GROUP_MAX_SIZE_MAX)
    @Schema(description = "Số lượng thành viên tối đa", example = "6")
    private Integer maxSize;

    @NotNull(message = MessageConstant.MATCHING_TARGET_DATE_REQUIRED)
    @Schema(description = "Ngày khởi hành mong muốn", example = "2026-10-15")
    private LocalDate targetDate;

    @NotNull(message = MessageConstant.MATCHING_DEADLINE_REQUIRED)
    @Schema(description = "Hạn chót đăng ký ghép nhóm", example = "2026-10-10T18:00:00")
    private LocalDateTime matchingDeadline;

    @NotNull(message = MessageConstant.MATCHING_SCHEDULED_START_REQUIRED)
    @Future(message = MessageConstant.MATCHING_SCHEDULED_START_FUTURE)
    @Schema(description = "Thời điểm xuất phát dự kiến", example = "2026-10-15T06:00:00")
    private LocalDateTime scheduledStartAt;
}
