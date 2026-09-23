package com.sep.treksphere.matching.service.impl;

import com.sep.treksphere.common.exception.AppException;
import com.sep.treksphere.common.exception.ErrorCode;
import com.sep.treksphere.matching.entity.GroupPeerReview;
import com.sep.treksphere.matching.enums.PeerReviewModerationStatus;
import com.sep.treksphere.matching.repository.GroupPeerReviewRepository;
import com.sep.treksphere.matching.service.TrustScoreService;
import com.sep.treksphere.user.entity.User;
import com.sep.treksphere.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;


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

        BigDecimal averageRating = sumOfPerReviewAverages.divide(
                BigDecimal.valueOf(reviewCount), INTERNAL_SCALE, RoundingMode.HALF_UP);

        BigDecimal scorePercent = averageRating.multiply(POINTS_PER_STAR);
        int rawScore = scorePercent.setScale(0, RoundingMode.HALF_UP).intValueExact();
        short trustScore = (short) Math.max(0, Math.min(100, rawScore));

        user.setTrustScore(trustScore);
        userRepository.save(user);
    }

    private BigDecimal perReviewAverage(GroupPeerReview review) {
        int sum = review.getActualEnduranceRating()
                + review.getPunctualityResponsibilityRating()
                + review.getFinancialFairnessRating();
        return BigDecimal.valueOf(sum).divide(RATING_CRITERIA_COUNT, INTERNAL_SCALE, RoundingMode.HALF_UP);
    }
}
