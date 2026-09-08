package com.sep.treksphere.matching.dto.request;

import com.sep.treksphere.common.constant.MessageConstant;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Request cập nhật thông tin nhóm ghép bạn đồng hành")
public class MatchingGroupUpdateRequest {

    @Size(min = 3, max = 100, message = MessageConstant.MATCHING_GROUP_NAME_SIZE)
    @Schema(description = "Tên nhóm ghép", example = "Lảo Thẩn Hunting Cloud Group")
    private String groupName;

    @Size(max = 2000, message = MessageConstant.MATCHING_GROUP_DESCRIPTION_MAX_LENGTH)
    @Schema(description = "Mô tả chi tiết nhóm ghép", example = "Tìm thêm 2 bạn đồng hành cùng trek Lảo Thẩn")
    private String description;

    @Min(value = 2, message = MessageConstant.MATCHING_GROUP_MAX_SIZE_MIN)
    @Max(value = 100, message = MessageConstant.MATCHING_GROUP_MAX_SIZE_MAX)
    @Schema(description = "Số lượng thành viên tối đa", example = "8")
    private Integer maxSize;

    @Schema(description = "Ngày đi dự kiến", example = "2026-10-15")
    private LocalDate targetDate;

    @Schema(description = "Hạn chót ghép nhóm", example = "2026-10-10T18:00:00")
    private LocalDateTime matchingDeadline;

    @Valid
    @Schema(description = "Thông tin cập nhật Custom Journey (chỉ áp dụng nếu nhóm nguồn là Custom Journey và chưa bị khóa)")
    private CustomJourneyUpdateRequest customJourney;
}
