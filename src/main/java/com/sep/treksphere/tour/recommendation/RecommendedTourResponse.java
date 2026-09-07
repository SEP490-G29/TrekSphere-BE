package com.sep.treksphere.tour.recommendation;

import com.sep.treksphere.tour.dto.response.TourSummaryResponse;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class RecommendedTourResponse {
    private TourSummaryResponse tour;
    private List<RecommendationReason> matchReasons;
}
