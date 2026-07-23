package com.sep.treksphere.dto.response;

import com.sep.treksphere.enums.tour.DifficultyLevel;
import com.sep.treksphere.enums.tour.TourStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TourDetailResponse {

    private String tourId;
    private String tourName;
    private String description;
    private DifficultyLevel difficulty;
    private String location;
    private Integer durationDays;
    private BigDecimal basePrice;
    private Integer minCapacity;
    private Integer maxCapacity;
    private BigDecimal totalDistanceKm;
    private String highlights;
    private String includes;
    private String excludes;
    private String coverImageUrl;
    private TourStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private String vendorId;
    private String vendorName;
    private String vendorLogoUrl;
    private String vendorContactEmail;
    private String vendorContactPhone;

    private String creatorId;
    private String creatorName;
    private String creatorEmail;

    private List<TourImageResponse> images;

    private List<TourScheduleResponse> schedules;

    private Double averageRating;
    private int totalReviews;
}
