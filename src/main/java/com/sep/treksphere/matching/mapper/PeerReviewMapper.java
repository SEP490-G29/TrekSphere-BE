package com.sep.treksphere.matching.mapper;

import com.sep.treksphere.matching.dto.response.PeerReviewResponse;
import com.sep.treksphere.matching.entity.GroupPeerReview;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class PeerReviewMapper {

    private static final BigDecimal THREE = new BigDecimal("3");

    public PeerReviewResponse toResponse(GroupPeerReview review) {
        if (review == null) {
            return null;
        }

        int sum = (review.getActualEnduranceRating() != null ? review.getActualEnduranceRating() : 0)
                + (review.getPunctualityResponsibilityRating() != null ? review.getPunctualityResponsibilityRating() : 0)
                + (review.getFinancialFairnessRating() != null ? review.getFinancialFairnessRating() : 0);
        BigDecimal average = BigDecimal.valueOf(sum).divide(THREE, 2, RoundingMode.HALF_UP);

        com.sep.treksphere.user.User revieweeUser = review.getRevieweeMatchingMember() != null ? review.getRevieweeMatchingMember().getUser() : null;
        boolean isLocked = revieweeUser != null && revieweeUser.getStatus() == com.sep.treksphere.user.UserStatus.LOCKED;

        return PeerReviewResponse.builder()
                .groupPeerReviewId(review.getGroupPeerReviewId())
                .groupTripId(review.getGroupTrip() != null ? review.getGroupTrip().getGroupTripId() : null)
                .revieweeMatchingMemberId(review.getRevieweeMatchingMember() != null
                        ? review.getRevieweeMatchingMember().getMatchingMemberId() : null)
                .revieweeUserId(isLocked ? null : (revieweeUser != null ? revieweeUser.getUserId() : null))
                .revieweeFullName(isLocked ? com.sep.treksphere.blog.BlogService.SYSTEM_USER_ANONYMOUS_NAME : (revieweeUser != null ? revieweeUser.getFullName() : null))
                .revieweeAvatarUrl(isLocked ? null : (revieweeUser != null ? revieweeUser.getAvatarUrl() : null))
                .actualEnduranceRating(review.getActualEnduranceRating())
                .punctualityResponsibilityRating(review.getPunctualityResponsibilityRating())
                .financialFairnessRating(review.getFinancialFairnessRating())
                .averageRating(average)
                .comment(review.getComment())
                .moderationStatus(review.getModerationStatus())
                .createdAt(review.getCreatedAt())
                .build();
    }
}
