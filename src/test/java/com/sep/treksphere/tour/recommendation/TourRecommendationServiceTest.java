package com.sep.treksphere.tour.recommendation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sep.treksphere.common.dto.PaginationResponse;
import com.sep.treksphere.matching.enums.MatchingGroupStatus;
import com.sep.treksphere.tour.DifficultyLevel;
import com.sep.treksphere.tour.Tour;
import com.sep.treksphere.tour.TourService;
import com.sep.treksphere.tour.dto.response.TourSummaryResponse;
import com.sep.treksphere.user.ExperienceLevel;
import com.sep.treksphere.user.User;
import com.sep.treksphere.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TourRecommendationServiceTest {

    private TourRecommendationRepository recommendationRepository;
    private TourService tourService;
    private UserRepository userRepository;
    private TourRecommendationService service;

    private final UUID userId = UUID.randomUUID();
    private final UUID tourId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        recommendationRepository = mock(TourRecommendationRepository.class);
        tourService = mock(TourService.class);
        userRepository = mock(UserRepository.class);
        service = new TourRecommendationService(
                recommendationRepository,
                tourService,
                userRepository,
                new ObjectMapper());
    }

    @Test
    void combinesProfileHistoryProgressionAndPopularityReasons() {
        User user = user(ExperienceLevel.EXPERT, DifficultyLevel.MODERATE, List.of(" Ha Giang "));
        Tour tour = tour(DifficultyLevel.EXTREME, "Sa Pa, Lao Cai");
        PageRequest pageable = PageRequest.of(0, 6);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(recommendationRepository.findCompletedTourLocations(userId))
                .thenReturn(List.of("Sa Pa, Lao Cai"));
        when(recommendationRepository.findCompletedTourDifficulties(userId))
                .thenReturn(List.of(
                        DifficultyLevel.EASY,
                        DifficultyLevel.EASY,
                        DifficultyLevel.HARD,
                        DifficultyLevel.HARD));
        when(recommendationRepository.findPersonalizedRecommendations(
                userId,
                "[\"Ha Giang\"]",
                "[\"Sa Pa, Lao Cai\"]",
                "MODERATE",
                "EXTREME",
                3,
                pageable))
                .thenReturn(new PageImpl<>(List.of(tour), pageable, 1));
        when(recommendationRepository.findPopularTourIds(
                eq(List.of(tourId)), any(), eq(2L)))
                .thenReturn(List.of(tourId));
        stubTourResponse(tour);

        PaginationResponse<RecommendedTourResponse> response =
                service.getRecommendations(userId, 0, 6);

        List<RecommendationReason> reasons = response.getContent().getFirst().getMatchReasons();
        assertTrue(reasons.contains(RecommendationReason.SIMILAR_TO_HISTORY));
        assertTrue(reasons.contains(RecommendationReason.SKILL_PROGRESSION));
        assertTrue(reasons.contains(RecommendationReason.EXPERIENCE));
        assertTrue(reasons.contains(RecommendationReason.POPULAR));
        assertFalse(reasons.contains(RecommendationReason.AREA));
        assertFalse(reasons.contains(RecommendationReason.DIFFICULTY));
    }

    @Test
    void usesProfilePreferencesAndNormalizesLocationValues() {
        User user = user(
                ExperienceLevel.INTERMEDIATE,
                DifficultyLevel.MODERATE,
                List.of("Ha Giang", " ha giang ", " "));
        Tour tour = tour(DifficultyLevel.MODERATE, "Dong Van, Ha Giang");
        PageRequest pageable = PageRequest.of(0, 6);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(recommendationRepository.findCompletedTourLocations(userId)).thenReturn(List.of());
        when(recommendationRepository.findCompletedTourDifficulties(userId)).thenReturn(List.of());
        when(recommendationRepository.findPersonalizedRecommendations(
                userId, "[\"Ha Giang\"]", "[]", "MODERATE", null, 1, pageable))
                .thenReturn(new PageImpl<>(List.of(tour), pageable, 1));
        when(recommendationRepository.findPopularTourIds(
                eq(List.of(tourId)), any(), eq(2L)))
                .thenReturn(List.of());
        stubTourResponse(tour);

        PaginationResponse<RecommendedTourResponse> response =
                service.getRecommendations(userId, 0, 6);

        List<RecommendationReason> reasons = response.getContent().getFirst().getMatchReasons();
        assertTrue(reasons.contains(RecommendationReason.AREA));
        assertTrue(reasons.contains(RecommendationReason.DIFFICULTY));
        assertTrue(reasons.contains(RecommendationReason.EXPERIENCE));
    }

    @Test
    void defaultsIncompleteProfileToEasyToursAndClampsPagination() {
        User user = user(null, null, List.of());
        Tour tour = tour(DifficultyLevel.EASY, "Ba Vi");
        PageRequest pageable = PageRequest.of(0, 20);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(recommendationRepository.findCompletedTourLocations(userId)).thenReturn(List.of());
        when(recommendationRepository.findCompletedTourDifficulties(userId)).thenReturn(List.of());
        when(recommendationRepository.findPersonalizedRecommendations(
                userId, "[]", "[]", null, null, 0, pageable))
                .thenReturn(new PageImpl<>(List.of(tour), pageable, 1));
        when(recommendationRepository.findPopularTourIds(
                eq(List.of(tourId)), any(), eq(2L)))
                .thenReturn(List.of());
        stubTourResponse(tour);

        PaginationResponse<RecommendedTourResponse> response =
                service.getRecommendations(userId, -5, 1000);

        assertEquals(List.of(RecommendationReason.DISCOVERY),
                response.getContent().getFirst().getMatchReasons());
        verify(recommendationRepository).findPersonalizedRecommendations(
                userId, "[]", "[]", null, null, 0, pageable);
    }

    @Test
    void infersSafeDifficultyFromCompletedTripsWhenProfileIsIncomplete() {
        User user = user(null, null, List.of());
        Tour tour = tour(DifficultyLevel.HARD, "Pu Luong");
        PageRequest pageable = PageRequest.of(0, 6);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(recommendationRepository.findCompletedTourLocations(userId))
                .thenReturn(List.of("Pu Luong"));
        when(recommendationRepository.findCompletedTourDifficulties(userId))
                .thenReturn(List.of(DifficultyLevel.HARD));
        when(recommendationRepository.findPersonalizedRecommendations(
                userId, "[]", "[\"Pu Luong\"]", null, null, 2, pageable))
                .thenReturn(new PageImpl<>(List.of(tour), pageable, 1));
        when(recommendationRepository.findPopularTourIds(
                eq(List.of(tourId)), any(), eq(2L)))
                .thenReturn(List.of());
        stubTourResponse(tour);

        PaginationResponse<RecommendedTourResponse> response =
                service.getRecommendations(userId, 0, 6);

        assertEquals(List.of(RecommendationReason.SIMILAR_TO_HISTORY),
                response.getContent().getFirst().getMatchReasons());
        verify(recommendationRepository).findPersonalizedRecommendations(
                userId, "[]", "[\"Pu Luong\"]", null, null, 2, pageable);
    }

    @Test
    void explainsBehaviorScheduleFlexibilityAndAvailableGroupSignals() {
        User user = user(null, null, List.of());
        Tour tour = tour(DifficultyLevel.EASY, "Dong Van, Ha Giang");
        PageRequest pageable = PageRequest.of(0, 6);
        TourRecommendationRepository.BehaviorSignalProjection behaviorSignal =
                mock(TourRecommendationRepository.BehaviorSignalProjection.class);

        when(behaviorSignal.getTourId()).thenReturn(tourId);
        when(behaviorSignal.getLocation()).thenReturn("Ha Giang");
        when(behaviorSignal.getDifficulty()).thenReturn(DifficultyLevel.EASY);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(recommendationRepository.findCompletedTourLocations(userId)).thenReturn(List.of());
        when(recommendationRepository.findCompletedTourDifficulties(userId)).thenReturn(List.of());
        when(recommendationRepository.findRecentPositiveBehaviorSignals(
                eq(userId), any(), anyList(), anyList()))
                .thenReturn(List.of(behaviorSignal));
        when(recommendationRepository.findPersonalizedRecommendations(
                userId, "[]", "[]", null, null, 0, pageable))
                .thenReturn(new PageImpl<>(List.of(tour), pageable, 1));
        when(recommendationRepository.findPopularTourIds(
                eq(List.of(tourId)), any(), eq(2L)))
                .thenReturn(List.of());
        when(recommendationRepository.findTourIdsWithFutureOpenSchedules(
                eq(List.of(tourId)), any(), eq(2L)))
                .thenReturn(List.of(tourId));
        when(recommendationRepository.findTourIdsWithAvailableGroups(
                eq(List.of(tourId)), any()))
                .thenReturn(List.of(tourId));
        stubTourResponse(tour);

        PaginationResponse<RecommendedTourResponse> response =
                service.getRecommendations(userId, 0, 6);

        List<RecommendationReason> reasons = response.getContent().getFirst().getMatchReasons();
        assertTrue(reasons.contains(RecommendationReason.BEHAVIOR));
        assertTrue(reasons.contains(RecommendationReason.SCHEDULE_FLEXIBILITY));
        assertTrue(reasons.contains(RecommendationReason.AVAILABLE_GROUP));
        assertFalse(reasons.contains(RecommendationReason.DISCOVERY));
    }

    private User user(
            ExperienceLevel experienceLevel,
            DifficultyLevel preferredDifficulty,
            List<String> preferredAreas) {
        User user = new User();
        user.setUserId(userId);
        user.setExperienceLevel(experienceLevel);
        user.setPreferredDifficulty(preferredDifficulty);
        user.setPreferredAreas(preferredAreas);
        return user;
    }

    private Tour tour(DifficultyLevel difficulty, String location) {
        Tour tour = new Tour();
        tour.setTourId(tourId);
        tour.setDifficulty(difficulty);
        tour.setLocation(location);
        return tour;
    }

    private void stubTourResponse(Tour tour) {
        when(tourService.loadFromPrices(anyList()))
                .thenReturn(Map.of(tourId, BigDecimal.valueOf(2_000_000)));
        when(tourService.toSummaryResponse(eq(tour), any()))
                .thenReturn(TourSummaryResponse.builder()
                        .tourId(tourId.toString())
                        .location(tour.getLocation())
                        .difficulty(tour.getDifficulty())
                        .build());
    }
}
