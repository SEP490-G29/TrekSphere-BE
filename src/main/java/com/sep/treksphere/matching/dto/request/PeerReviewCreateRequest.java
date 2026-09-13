package com.sep.treksphere.matching.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PeerReviewCreateRequest {

    /**
     * ID thành viên (MatchingMember) hoặc User ID của người được đánh giá.
     * Cho phép client truyền revieweeMemberId hoặc revieweeUserId.
     */
    private UUID revieweeMemberId;
    private UUID revieweeUserId;

    @NotNull(message = "Điểm thể lực thực tế không được để trống")
    @Min(value = 1, message = "Điểm đánh giá phải từ 1 đến 5")
    @Max(value = 5, message = "Điểm đánh giá phải từ 1 đến 5")
    private Short actualEnduranceRating;

    @NotNull(message = "Điểm đúng giờ và trách nhiệm không được để trống")
    @Min(value = 1, message = "Điểm đánh giá phải từ 1 đến 5")
    @Max(value = 5, message = "Điểm đánh giá phải từ 1 đến 5")
    private Short punctualityResponsibilityRating;

    @NotNull(message = "Điểm minh bạch tài chính không được để trống")
    @Min(value = 1, message = "Điểm đánh giá phải từ 1 đến 5")
    @Max(value = 5, message = "Điểm đánh giá phải từ 1 đến 5")
    private Short financialFairnessRating;

    @Size(max = 1000, message = "Nhận xét không được vượt quá 1000 ký tự")
    private String comment;
}
