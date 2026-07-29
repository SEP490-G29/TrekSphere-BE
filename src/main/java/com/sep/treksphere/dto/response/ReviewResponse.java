package com.sep.treksphere.dto.response;

import com.sep.treksphere.enums.blog.ReviewStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReviewResponse {

    // Review info
    private String reviewId;
    private Integer rating;
    private String content;
    private ReviewStatus status;

    // Thông tin người đánh giá
    private String userId;
    private String userFullName;
    private String userAvatarUrl;

    // Thông tin tour
    private String tourId;
    private String tourName;
    private String tourCoverImageUrl;

    // Thông tin booking liên quan
    private String bookingId;
    private String bookingCode;

    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
