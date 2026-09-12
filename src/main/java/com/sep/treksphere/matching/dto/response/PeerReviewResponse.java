package com.sep.treksphere.matching.dto.response;

import com.sep.treksphere.matching.enums.PeerReviewModerationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PeerReviewResponse {

    private UUID groupPeerReviewId;
    private UUID groupTripId;

    // Reviewee information (Người được đánh giá)
    private UUID revieweeMatchingMemberId;
    private UUID revieweeUserId;
    private String revieweeFullName;
    private String revieweeAvatarUrl;

    // Ratings (3 tiêu chí)
    private Short actualEnduranceRating;
    private Short punctualityResponsibilityRating;
    private Short financialFairnessRating;
    private BigDecimal averageRating;

    private String comment;
    private PeerReviewModerationStatus moderationStatus;
    private LocalDateTime createdAt;
}
