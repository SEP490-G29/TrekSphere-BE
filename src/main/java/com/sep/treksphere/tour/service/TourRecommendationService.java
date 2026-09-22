package com.sep.treksphere.tour.recommendation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sep.treksphere.common.dto.PaginationResponse;
import com.sep.treksphere.common.exception.AppException;
import com.sep.treksphere.common.exception.ErrorCode;
import com.sep.treksphere.matching.enums.MatchingGroupStatus;
import com.sep.treksphere.tour.DifficultyLevel;
import com.sep.treksphere.tour.Tour;
import com.sep.treksphere.tour.TourService;
import com.sep.treksphere.user.ExperienceLevel;
import com.sep.treksphere.user.User;
import com.sep.treksphere.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TourRecommendationService {

    private static final int DEFAULT_MAX_DIFFICULTY_RANK = 0;
    private static final int MAX_PAGE_SIZE = 20;
    private static final long POPULARITY_GROUP_THRESHOLD = 2;
    private static final long FLEXIBLE_SCHEDULE_THRESHOLD = 2;
    private static final int BEHAVIOR_REASON_WINDOW_DAYS = 90;
    private static final List<MatchingGroupStatus> POPULARITY_STATUSES = List.of(
            MatchingGroupStatus.OPEN,
            MatchingGroupStatus.FULL,
            MatchingGroupStatus.CLOSED,
            MatchingGroupStatus.IN_PROGRESS,
            MatchingGroupStatus.COMPLETED
    );
    private static final List<TourBehaviorEventType> POSITIVE_BEHAVIOR_TYPES = List.of(
            TourBehaviorEventType.VIEW,
            TourBehaviorEventType.CLICK,
            TourBehaviorEventType.SAVE
    );
    private static final List<TourBehaviorEventType> NEGATIVE_BEHAVIOR_TYPES = List.of(
            TourBehaviorEventType.UNSAVE,
            TourBehaviorEventType.DISMISS
    );

    private final TourRecommendationRepository recommendationRepository;
    private final TourService tourService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public PaginationResponse<RecommendedTourResponse> getRecommendations(
            UUID userId, int page, int size) {
        User user = userRepository.findById(userId)
                .filter(value -> !Boolean.TRUE.equals(value.getIsDeleted()))
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        List<String> preferredAreas = normalizeLocations(user.getPreferredAreas());
        List<String> historyLocations = normalizeLocations(
                recommendationRepository.findCompletedTourLocations(userId));
        List<DifficultyLevel> completedDifficulties = nullSafeList(
                recommendationRepository.findCompletedTourDifficulties(userId));
        List<TourRecommendationRepository.BehaviorSignalProjection> behaviorSignals =
                recommendationRepository.findRecentPositiveBehaviorSignals(
                        userId,
                        LocalDateTime.now().minusDays(BEHAVIOR_REASON_WINDOW_DAYS),
                        POSITIVE_BEHAVIOR_TYPES,
                        NEGATIVE_BEHAVIOR_TYPES);
        List<String> behaviorLocations = normalizeLocations(behaviorSignals.stream()
                .map(TourRecommendationRepository.BehaviorSignalProjection::getLocation)
                .toList());
        Set<DifficultyLevel> behaviorDifficulties = behaviorSignals.stream()
                .map(TourRecommendationRepository.BehaviorSignalProjection::getDifficulty)
                .filter(difficulty -> difficulty != null)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        Set<UUID> behaviorTourIds = behaviorSignals.stream()
                .map(TourRecommendationRepository.BehaviorSignalProjection::getTourId)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());

        int maxDifficultyRank = resolveMaxDifficultyRank(user, completedDifficulties);
        DifficultyLevel progressionDifficulty = determineProgressionDifficulty(
                completedDifficulties, maxDifficultyRank);

        Page<Tour> result = recommendationRepository.findPersonalizedRecommendations(
                userId,
                toJson(preferredAreas),
                toJson(historyLocations),
                enumName(user.getPreferredDifficulty()),
                enumName(progressionDifficulty),
                maxDifficultyRank,
                PageRequest.of(normalizePage(page), normalizeSize(size)));

        List<UUID> recommendedTourIds = result.getContent().stream()
                .map(Tour::getTourId)
                .toList();
        Set<UUID> popularTourIds = findPopularTourIds(recommendedTourIds);
        Set<UUID> flexibleScheduleTourIds = findFlexibleScheduleTourIds(recommendedTourIds);
        Set<UUID> availableGroupTourIds = findAvailableGroupTourIds(recommendedTourIds);
        List<RecommendedTourResponse> content = result.getContent().stream()
                .map(tour -> RecommendedTourResponse.builder()
                        .tour(tourService.toSummaryResponse(tour))
                        .matchReasons(matchReasons(
                                tour,
                                user,
                                preferredAreas,
                                behaviorTourIds,
                                behaviorLocations,
                                behaviorDifficulties,
                                historyLocations,
                                progressionDifficulty,
                                flexibleScheduleTourIds,
                                availableGroupTourIds,
                                popularTourIds))
                        .build())
                .toList();

        return PaginationResponse.<RecommendedTourResponse>builder()
                .content(content)
                .pageNumber(result.getNumber())
                .pageSize(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .last(result.isLast())
                .build();
    }

    private List<RecommendationReason> matchReasons(
            Tour tour,
            User user,
            List<String> preferredAreas,
            Set<UUID> behaviorTourIds,
            List<String> behaviorLocations,
            Set<DifficultyLevel> behaviorDifficulties,
            List<String> historyLocations,
            DifficultyLevel progressionDifficulty,
            Set<UUID> flexibleScheduleTourIds,
            Set<UUID> availableGroupTourIds,
            Set<UUID> popularTourIds) {
        List<RecommendationReason> reasons = new ArrayList<>();
        String location = tour.getLocation();

        if (matchesAnyLocation(location, preferredAreas)) {
            reasons.add(RecommendationReason.AREA);
        }
        if (behaviorTourIds.contains(tour.getTourId())
                || matchesAnyLocation(location, behaviorLocations)
                || behaviorDifficulties.contains(tour.getDifficulty())) {
            reasons.add(RecommendationReason.BEHAVIOR);
        }
        if (matchesAnyLocation(location, historyLocations)) {
            reasons.add(RecommendationReason.SIMILAR_TO_HISTORY);
        }
        if (user.getPreferredDifficulty() == tour.getDifficulty()) {
            reasons.add(RecommendationReason.DIFFICULTY);
        }
        if (progressionDifficulty == tour.getDifficulty()) {
            reasons.add(RecommendationReason.SKILL_PROGRESSION);
        }
        if (user.getExperienceLevel() != null
                && difficultyRank(tour.getDifficulty()) <= experienceRank(user.getExperienceLevel())) {
            reasons.add(RecommendationReason.EXPERIENCE);
        }
        if (flexibleScheduleTourIds.contains(tour.getTourId())) {
            reasons.add(RecommendationReason.SCHEDULE_FLEXIBILITY);
        }
        if (availableGroupTourIds.contains(tour.getTourId())) {
            reasons.add(RecommendationReason.AVAILABLE_GROUP);
        }
        if (popularTourIds.contains(tour.getTourId())) {
            reasons.add(RecommendationReason.POPULAR);
        }
        if (reasons.isEmpty()) {
            reasons.add(RecommendationReason.DISCOVERY);
        }
        return List.copyOf(reasons);
    }

    private Set<UUID> findPopularTourIds(List<UUID> tourIds) {
        if (tourIds.isEmpty()) {
            return Set.of();
        }
        return new HashSet<>(recommendationRepository.findPopularTourIds(
                tourIds, POPULARITY_STATUSES, POPULARITY_GROUP_THRESHOLD));
    }

    private Set<UUID> findFlexibleScheduleTourIds(List<UUID> tourIds) {
        if (tourIds.isEmpty()) {
            return Set.of();
        }
        return new HashSet<>(recommendationRepository.findTourIdsWithFutureOpenSchedules(
                tourIds, LocalDate.now(), FLEXIBLE_SCHEDULE_THRESHOLD));
    }

    private Set<UUID> findAvailableGroupTourIds(List<UUID> tourIds) {
        if (tourIds.isEmpty()) {
            return Set.of();
        }
        return new HashSet<>(recommendationRepository.findTourIdsWithAvailableGroups(
                tourIds, LocalDate.now()));
    }

    private DifficultyLevel determineProgressionDifficulty(
            List<DifficultyLevel> completedDifficulties, int maxDifficultyRank) {
        DifficultyLevel highestCompleted = completedDifficulties.stream()
                .filter(difficulty -> difficulty != null)
                .max((left, right) -> Integer.compare(
                        difficultyRank(left), difficultyRank(right)))
                .orElse(null);
        if (highestCompleted == null) {
            return null;
        }

        long completedAtHighestLevel = completedDifficulties.stream()
                .filter(highestCompleted::equals)
                .count();
        int nextRank = difficultyRank(highestCompleted) + 1;
        if (completedAtHighestLevel < 2 || nextRank > maxDifficultyRank) {
            return null;
        }
        return difficultyForRank(nextRank);
    }

    private int resolveMaxDifficultyRank(
            User user, List<DifficultyLevel> completedDifficulties) {
        if (user.getExperienceLevel() != null) {
            return experienceRank(user.getExperienceLevel());
        }
        if (user.getPreferredDifficulty() != null) {
            return difficultyRank(user.getPreferredDifficulty());
        }
        return completedDifficulties.stream()
                .filter(difficulty -> difficulty != null)
                .mapToInt(this::difficultyRank)
                .max()
                .orElse(DEFAULT_MAX_DIFFICULTY_RANK);
    }

    private int experienceRank(ExperienceLevel experienceLevel) {
        return switch (experienceLevel) {
            case BEGINNER -> 0;
            case INTERMEDIATE -> 1;
            case ADVANCED -> 2;
            case EXPERT -> 3;
        };
    }

    private int difficultyRank(DifficultyLevel difficulty) {
        if (difficulty == null) {
            return Integer.MAX_VALUE;
        }
        return switch (difficulty) {
            case EASY -> 0;
            case MODERATE -> 1;
            case HARD -> 2;
            case EXTREME -> 3;
        };
    }

    private DifficultyLevel difficultyForRank(int rank) {
        return switch (rank) {
            case 0 -> DifficultyLevel.EASY;
            case 1 -> DifficultyLevel.MODERATE;
            case 2 -> DifficultyLevel.HARD;
            case 3 -> DifficultyLevel.EXTREME;
            default -> null;
        };
    }

    private boolean matchesAnyLocation(String tourLocation, List<String> targetLocations) {
        if (tourLocation == null || tourLocation.isBlank()) {
            return false;
        }
        String normalizedTourLocation = normalizeForComparison(tourLocation);
        return targetLocations.stream()
                .map(this::normalizeForComparison)
                .anyMatch(location -> normalizedTourLocation.contains(location)
                        || location.contains(normalizedTourLocation));
    }

    private List<String> normalizeLocations(List<String> locations) {
        if (locations == null || locations.isEmpty()) {
            return List.of();
        }
        Map<String, String> uniqueLocations = new LinkedHashMap<>();
        locations.stream()
                .filter(location -> location != null && !location.isBlank())
                .map(String::strip)
                .forEach(location -> uniqueLocations.putIfAbsent(
                        normalizeForComparison(location), location));
        return List.copyOf(uniqueLocations.values());
    }

    private String normalizeForComparison(String value) {
        return value.strip().toLowerCase(Locale.ROOT);
    }

    private <T> List<T> nullSafeList(List<T> values) {
        return values == null ? List.of() : values;
    }

    private String enumName(Enum<?> value) {
        return value == null ? null : value.name();
    }

    private int normalizePage(int page) {
        return Math.max(page, 0);
    }

    private int normalizeSize(int size) {
        return Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
    }

    private String toJson(List<String> locations) {
        try {
            return objectMapper.writeValueAsString(locations);
        } catch (JsonProcessingException exception) {
            throw new AppException(
                    ErrorCode.VALIDATION_ERROR,
                    "Không thể xử lý dữ liệu cá nhân hóa tour.");
        }
    }
}
