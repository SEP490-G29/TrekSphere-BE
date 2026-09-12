package com.sep.treksphere.matching.service;

import com.sep.treksphere.matching.entity.GroupPeerReview;
import com.sep.treksphere.matching.enums.PeerReviewModerationStatus;
import com.sep.treksphere.matching.repository.GroupPeerReviewRepository;
import com.sep.treksphere.matching.service.impl.TrustScoreServiceImpl;
import com.sep.treksphere.user.User;
import com.sep.treksphere.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrustScoreServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private GroupPeerReviewRepository groupPeerReviewRepository;

    @InjectMocks
    private TrustScoreServiceImpl trustScoreService;

    private UUID userId;
    private User testUser;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        testUser = new User();
        testUser.setUserId(userId);
        testUser.setTrustScore((short) 100);
        testUser.setTrustReviewCount(0);
    }

    @Test
    @DisplayName("Chưa có review nào: Mặc định 100 điểm")
    void recalculateTrustScore_noReviews_defaultsTo100() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(groupPeerReviewRepository.findByRevieweeMatchingMember_User_UserIdAndModerationStatusAndIsDeletedFalse(
                eq(userId), eq(PeerReviewModerationStatus.VISIBLE))).thenReturn(Collections.emptyList());

        trustScoreService.recalculateTrustScore(userId);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User saved = userCaptor.getValue();
        assertThat(saved.getTrustScore()).isEqualTo((short) 100);
        assertThat(saved.getTrustReviewCount()).isEqualTo(0);
        assertThat(saved.getTrustCalculatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Nhận toàn bộ 5 sao: Điểm uy tín giữ nguyên 100")
    void recalculateTrustScore_allFiveStars_keeps100() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        GroupPeerReview r1 = createReview((short) 5, (short) 5, (short) 5);
        GroupPeerReview r2 = createReview((short) 5, (short) 5, (short) 5);

        when(groupPeerReviewRepository.findByRevieweeMatchingMember_User_UserIdAndModerationStatusAndIsDeletedFalse(
                eq(userId), eq(PeerReviewModerationStatus.VISIBLE))).thenReturn(List.of(r1, r2));

        trustScoreService.recalculateTrustScore(userId);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User saved = userCaptor.getValue();
        assertThat(saved.getTrustScore()).isEqualTo((short) 100);
        assertThat(saved.getTrustReviewCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("Nhận đánh giá thấp: Bị trừ điểm tương ứng (trung bình 4 sao = 80 điểm)")
    void recalculateTrustScore_mixedRatings_reducedScore() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        // Review 1: 4, 4, 4 -> avg = 4.0
        GroupPeerReview r1 = createReview((short) 4, (short) 4, (short) 4);
        // Review 2: 4, 4, 4 -> avg = 4.0
        GroupPeerReview r2 = createReview((short) 4, (short) 4, (short) 4);

        when(groupPeerReviewRepository.findByRevieweeMatchingMember_User_UserIdAndModerationStatusAndIsDeletedFalse(
                eq(userId), eq(PeerReviewModerationStatus.VISIBLE))).thenReturn(List.of(r1, r2));

        trustScoreService.recalculateTrustScore(userId);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User saved = userCaptor.getValue();
        assertThat(saved.getTrustScore()).isEqualTo((short) 80);
        assertThat(saved.getTrustReviewCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("Nhận đánh giá 1 sao: Điểm uy tín còn 20 điểm")
    void recalculateTrustScore_allOneStar_score20() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        GroupPeerReview r1 = createReview((short) 1, (short) 1, (short) 1);

        when(groupPeerReviewRepository.findByRevieweeMatchingMember_User_UserIdAndModerationStatusAndIsDeletedFalse(
                eq(userId), eq(PeerReviewModerationStatus.VISIBLE))).thenReturn(List.of(r1));

        trustScoreService.recalculateTrustScore(userId);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User saved = userCaptor.getValue();
        assertThat(saved.getTrustScore()).isEqualTo((short) 20);
        assertThat(saved.getTrustReviewCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Nhận đánh giá hỗn hợp có điểm thập phân: Làm tròn chính xác (3.5 sao = 70 điểm)")
    void recalculateTrustScore_mixedDecimals_roundsCorrectly() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        // Review 1: 3, 4, 3.5 avg
        GroupPeerReview r1 = createReview((short) 3, (short) 4, (short) 3); // avg = 3.333
        GroupPeerReview r2 = createReview((short) 4, (short) 4, (short) 4); // avg = 4.0
        // Overall avg = (3.333 + 4.0)/2 = 3.667 -> 3.667 * 20 = 73.33 -> 73

        when(groupPeerReviewRepository.findByRevieweeMatchingMember_User_UserIdAndModerationStatusAndIsDeletedFalse(
                eq(userId), eq(PeerReviewModerationStatus.VISIBLE))).thenReturn(List.of(r1, r2));

        trustScoreService.recalculateTrustScore(userId);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User saved = userCaptor.getValue();
        assertThat(saved.getTrustScore()).isEqualTo((short) 73);
        assertThat(saved.getTrustReviewCount()).isEqualTo(2);
    }

    private GroupPeerReview createReview(short endurance, short punctuality, short financial) {
        GroupPeerReview r = new GroupPeerReview();
        r.setActualEnduranceRating(endurance);
        r.setPunctualityResponsibilityRating(punctuality);
        r.setFinancialFairnessRating(financial);
        return r;
    }
}
