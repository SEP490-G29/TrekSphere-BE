package com.sep.treksphere.dto.response;

import com.sep.treksphere.enums.tour.DifficultyLevel;
import com.sep.treksphere.enums.tour.TourStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TourSummaryResponse {

    private String tourId;
    private String tourName;
    private String location;
    private Integer durationDays;
    private BigDecimal basePrice;
    private Integer minCapacity;
    private Integer maxCapacity;
    private BigDecimal totalDistanceKm;
    private DifficultyLevel difficulty;
    private TourStatus status;
    private String coverImageUrl;

    private String vendorId;
    private String vendorName;

    private Double averageRating;
    private int totalReviews;

    private LocalDateTime createdAt;
}
