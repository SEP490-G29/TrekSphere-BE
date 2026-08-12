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
    private String rejectionReason;
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

    private List<CancellationPolicyResponse> cancellationPolicies;

    /** Chính sách trả đủ/đặt cọc hiện hành để Trekker chọn đúng khi đặt tour. */
    private TourPaymentPolicyResponse paymentPolicy;

    /** Điều kiện tuổi, thể lực, sức khỏe, trang bị và giấy tờ của tour. */
    private TourParticipationPolicyResponse participationPolicy;

    /** Tour vẫn public khi false, nhưng không được tạo booking online. */
    private Boolean onlineBookingEnabled;

    /** Lý do cụ thể để cả Trekker và Vendor biết bước cấu hình còn thiếu. */
    private String onlineBookingDisabledReason;

    /** Chi phí không hoàn lại của tour, dùng để giải thích chính sách trước khi đặt. */
    private BigDecimal nonRefundableCost;

    private Double averageRating;
    private int totalReviews;
}
