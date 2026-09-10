package com.sep.treksphere.tour.recommendation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sep.treksphere.common.dto.PaginationResponse;
import com.sep.treksphere.common.exception.AppException;
import com.sep.treksphere.common.exception.ErrorCode;
import com.sep.treksphere.tour.Tour;
import com.sep.treksphere.tour.TourRepository;
import com.sep.treksphere.tour.TourService;
import com.sep.treksphere.user.User;
import com.sep.treksphere.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TourRecommendationService {

    private final TourRepository tourRepository;
    private final TourService tourService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public PaginationResponse<RecommendedTourResponse> getRecommendations(
            UUID userId, int page, int size) {
        User user = userRepository.findById(userId)
                .filter(value -> !Boolean.TRUE.equals(value.getIsDeleted()))
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        List<String> areas = user.getPreferredAreas() == null ? List.of() : user.getPreferredAreas();
        boolean usePreferences = !areas.isEmpty() || user.getPreferredDifficulty() != null;
        String preferredDifficulty = user.getPreferredDifficulty() == null
                ? null : user.getPreferredDifficulty().name();
        Integer maxDifficultyRank = user.getExperienceLevel() == null
                ? null : user.getExperienceLevel().ordinal();

        Page<Tour> result = tourRepository.findRecommendedTours(
                toJson(areas), preferredDifficulty, maxDifficultyRank, usePreferences,
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 20)));
        Map<UUID, BigDecimal> prices = tourService.loadFromPrices(result.getContent());
        List<RecommendedTourResponse> content = result.getContent().stream()
                .map(tour -> RecommendedTourResponse.builder()
                        .tour(tourService.toSummaryResponse(tour, prices.get(tour.getTourId())))
                        .matchReasons(matchReasons(tour, user, areas))
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

    private List<RecommendationReason> matchReasons(Tour tour, User user, List<String> areas) {
        List<RecommendationReason> reasons = new ArrayList<>();
        String location = tour.getLocation().toLowerCase(Locale.ROOT);
        if (areas.stream().anyMatch(area -> location.contains(area.toLowerCase(Locale.ROOT)))) {
            reasons.add(RecommendationReason.AREA);
        }
        if (user.getPreferredDifficulty() == tour.getDifficulty()) {
            reasons.add(RecommendationReason.DIFFICULTY);
        }
        if (user.getExperienceLevel() != null
                && tour.getDifficulty().ordinal() <= user.getExperienceLevel().ordinal()) {
            reasons.add(RecommendationReason.EXPERIENCE);
        }
        if (reasons.isEmpty()) {
            reasons.add(RecommendationReason.POPULAR);
        }
        return reasons;
    }

    private String toJson(List<String> areas) {
        try {
            return objectMapper.writeValueAsString(areas);
        } catch (JsonProcessingException ex) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Không thể xử lý khu vực yêu thích.");
        }
    }
}
