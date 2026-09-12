package com.sep.treksphere.matching.service.impl;

import com.sep.treksphere.common.exception.AppException;
import com.sep.treksphere.common.exception.ErrorCode;
import com.sep.treksphere.matching.entity.GroupPeerReview;
import com.sep.treksphere.matching.enums.PeerReviewModerationStatus;
import com.sep.treksphere.matching.repository.GroupPeerReviewRepository;
import com.sep.treksphere.matching.service.TrustScoreService;
import com.sep.treksphere.user.User;
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
public class TrustScoreServiceImpl implements TrustScoreService {

    public static final short DEFAULT_TRUST_SCORE = 100;
    private static final BigDecimal POINTS_PER_STAR = new BigDecimal("20"); // 5 sao = 100 điểm (100 / 5)
    private static final BigDecimal RATING_CRITERIA_COUNT = new BigDecimal("3");
    private static final int INTERNAL_SCALE = 10;

    private final UserRepository userRepository;
    private final GroupPeerReviewRepository groupPeerReviewRepository;

    @Override
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
            user.setTrustScore(DEFAULT_TRUST_SCORE);
            userRepository.save(user);
            return;
        }

        BigDecimal sumOfPerReviewAverages = BigDecimal.ZERO;
        for (GroupPeerReview review : visibleReviews) {
            sumOfPerReviewAverages = sumOfPerReviewAverages.add(perReviewAverage(review));
        }

        // Điểm đánh giá trung bình thực tế (thang 1..5 sao)
        BigDecimal averageRating = sumOfPerReviewAverages.divide(
                BigDecimal.valueOf(reviewCount), INTERNAL_SCALE, RoundingMode.HALF_UP);

        // Quy đổi ra thang điểm 0..100 (5 sao = 100, 4 sao = 80, 3 sao = 60, 1 sao = 20)
        BigDecimal scorePercent = averageRating.multiply(POINTS_PER_STAR);
        int rawScore = scorePercent.setScale(0, RoundingMode.HALF_UP).intValueExact();
        short trustScore = (short) Math.max(0, Math.min(100, rawScore));

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
