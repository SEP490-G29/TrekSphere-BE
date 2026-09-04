package com.sep.treksphere.matching.service;

import com.sep.treksphere.matching.entity.GroupPeerReview;
import com.sep.treksphere.matching.enums.PeerReviewModerationStatus;
import com.sep.treksphere.matching.repository.GroupPeerReviewRepository;

import com.sep.treksphere.user.User;
import com.sep.treksphere.common.exception.AppException;
import com.sep.treksphere.common.exception.ErrorCode;
import com.sep.treksphere.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Tính users.trust_score theo công thức Bayesian ở db_refactor_v3.md Mục 8.9.
 * trust_score/trust_review_count/trust_calculated_at là cache/projection — source of truth
 * luôn là group_peer_review, nên hàm này luôn tính lại từ đầu (không cộng dồn incremental)
 * để đảm bảo tái tạo đúng dù review bị xoá hoặc đổi moderation_status.
 */
@Service
@RequiredArgsConstructor
public class TrustScoreService {

    /** m — điểm trung tính dùng làm prior Bayesian, thang 1..5. */
    private static final BigDecimal PRIOR_MEAN = new BigDecimal("3.5");
    /** C — trọng số của prior, tương đương "3 review ảo" ở điểm trung tính. */
    private static final BigDecimal PRIOR_WEIGHT = new BigDecimal("3");
    private static final BigDecimal RATING_SCALE_MIN = BigDecimal.ONE;
    private static final BigDecimal RATING_SCALE_SPAN = new BigDecimal("4"); // 5 - 1
    private static final BigDecimal PERCENT_SCALE = new BigDecimal("100");
    private static final BigDecimal RATING_CRITERIA_COUNT = new BigDecimal("3");
    private static final int INTERNAL_SCALE = 10;

    private final UserRepository userRepository;
    private final GroupPeerReviewRepository groupPeerReviewRepository;

    @Transactional
    public void recalculateTrustScore(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        List<GroupPeerReview> visibleReviews = groupPeerReviewRepository
                .findByRevieweeMatchingMember_User_UserIdAndModerationStatusAndIsDeletedFalse(
                        userId, PeerReviewModerationStatus.VISIBLE);

        int reviewCount = visibleReviews.size();
        user.setTrustReviewCount(reviewCount);
        user.setTrustCalculatedAt(LocalDateTime.now());

        if (reviewCount == 0) {
            user.setTrustScore(null);
            userRepository.save(user);
            return;
        }

        BigDecimal sumOfPerReviewAverages = BigDecimal.ZERO;
        for (GroupPeerReview review : visibleReviews) {
            sumOfPerReviewAverages = sumOfPerReviewAverages.add(perReviewAverage(review));
        }

        // adjusted = (C x m + Sum(r)) / (C + n)
        BigDecimal numerator = PRIOR_MEAN.multiply(PRIOR_WEIGHT).add(sumOfPerReviewAverages);
        BigDecimal denominator = PRIOR_WEIGHT.add(BigDecimal.valueOf(reviewCount));
        BigDecimal adjusted = numerator.divide(denominator, INTERNAL_SCALE, RoundingMode.HALF_UP);

        // trust_score = ROUND((adjusted - 1) / 4 x 100), thang 0..100
        BigDecimal scorePercent = adjusted.subtract(RATING_SCALE_MIN)
                .divide(RATING_SCALE_SPAN, INTERNAL_SCALE, RoundingMode.HALF_UP)
                .multiply(PERCENT_SCALE);

        short trustScore = (short) scorePercent.setScale(0, RoundingMode.HALF_UP).intValueExact();

        user.setTrustScore(trustScore);
        userRepository.save(user);
    }

    /** r = trung bình 3 tiêu chí của một review, thang 1..5. */
    private BigDecimal perReviewAverage(GroupPeerReview review) {
        int sum = review.getActualEnduranceRating()
                + review.getPunctualityResponsibilityRating()
                + review.getFinancialFairnessRating();
        return BigDecimal.valueOf(sum).divide(RATING_CRITERIA_COUNT, INTERNAL_SCALE, RoundingMode.HALF_UP);
    }
}
