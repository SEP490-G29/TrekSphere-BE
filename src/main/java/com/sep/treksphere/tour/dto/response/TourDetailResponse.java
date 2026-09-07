package com.sep.treksphere.tour.dto.response;

import com.sep.treksphere.tour.checkpoint.TourCheckpointResponse;
import com.sep.treksphere.tour.image.TourImageResponse;
import com.sep.treksphere.tour.schedule.TourScheduleResponse;
import com.sep.treksphere.tour.DifficultyLevel;
import com.sep.treksphere.tour.TourStatus;
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
    private BigDecimal fromPrice;
    private Integer minCapacity;
    private Integer maxCapacity;
    private BigDecimal totalDistanceKm;
    private String highlights;
    private String includes;
    private String excludes;
    private String coverImageUrl;
    private TourStatus status;
    private String hiddenReason;
    private LocalDateTime publishedAt;
    private LocalDateTime hiddenAt;
    private String hiddenBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private String vendorId;
    private String vendorManagerId;
    private String vendorName;
    private String vendorLogoUrl;
    private String vendorContactEmail;
    private String vendorContactPhone;

    private String creatorId;
    private String creatorName;
    private String creatorEmail;

    private List<TourImageResponse> images;

    private List<TourCheckpointResponse> checkpoints;

    private List<TourScheduleResponse> schedules;

    private boolean publishable;
    private List<String> publishReadinessErrors;
}
