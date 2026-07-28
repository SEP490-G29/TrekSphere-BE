package com.sep.treksphere.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReviewSummaryResponse {

    // Thống kê tổng hợp
    private Double averageRating;
    private int totalReviews;

    // Phân bổ rating (FE hiển thị bar chart)
    private int fiveStar;
    private int fourStar;
    private int threeStar;
    private int twoStar;
    private int oneStar;

    // Danh sách reviews có phân trang
    private PaginationResponse<ReviewResponse> reviews;
}
