package com.sep.treksphere.tour.dto.response;

import com.sep.treksphere.tour.enums.RecommendationReason;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class RecommendedTourResponse {
    private TourSummaryResponse tour;
    private List<RecommendationReason> matchReasons;
}
