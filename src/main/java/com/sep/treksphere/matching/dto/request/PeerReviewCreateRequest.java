package com.sep.treksphere.matching.dto.request;

import com.sep.treksphere.common.constant.MessageConstant;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Request đánh giá chéo thành viên (Peer Review) sau chuyến đi")
public class PeerReviewCreateRequest {

    @Schema(description = "ID thành viên (MatchingMember) được đánh giá")
    private UUID revieweeMemberId;

    @Schema(description = "User ID của người được đánh giá (nếu không có revieweeMemberId)")
    private UUID revieweeUserId;

    @NotNull(message = MessageConstant.PEER_REVIEW_FITNESS_RATING_REQUIRED)
    @Min(value = 1, message = MessageConstant.PEER_REVIEW_RATING_RANGE)
    @Max(value = 5, message = MessageConstant.PEER_REVIEW_RATING_RANGE)
    @Schema(description = "Điểm thể lực thực tế (1-5 sao)", example = "5")
    private Short actualEnduranceRating;

    @NotNull(message = MessageConstant.PEER_REVIEW_PUNCTUALITY_RATING_REQUIRED)
    @Min(value = 1, message = MessageConstant.PEER_REVIEW_RATING_RANGE)
    @Max(value = 5, message = MessageConstant.PEER_REVIEW_RATING_RANGE)
    @Schema(description = "Điểm đúng giờ và trách nhiệm (1-5 sao)", example = "5")
    private Short punctualityResponsibilityRating;

    @NotNull(message = MessageConstant.PEER_REVIEW_FINANCIAL_RATING_REQUIRED)
    @Min(value = 1, message = MessageConstant.PEER_REVIEW_RATING_RANGE)
    @Max(value = 5, message = MessageConstant.PEER_REVIEW_RATING_RANGE)
    @Schema(description = "Điểm minh bạch tài chính (1-5 sao)", example = "5")
    private Short financialFairnessRating;

    @Size(max = 1000, message = MessageConstant.PEER_REVIEW_FEEDBACK_MAX_LENGTH)
    @Schema(description = "Nhận xét chi tiết về thành viên đồng hành", example = "Bạn đi rất đúng giờ, hỗ trợ đồng đội nhiệt tình suốt cung đường.")
    private String comment;
}
