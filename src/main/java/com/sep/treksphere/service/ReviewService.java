package com.sep.treksphere.service;

import com.sep.treksphere.dto.request.CreateReviewRequest;
import com.sep.treksphere.dto.request.ReviewFilterRequest;
import com.sep.treksphere.dto.request.UpdateReviewStatusRequest;
import com.sep.treksphere.dto.response.ReviewResponse;
import com.sep.treksphere.dto.response.ReviewSummaryResponse;
import com.sep.treksphere.security.CustomUserDetails;

import java.util.UUID;

public interface ReviewService {

    ReviewSummaryResponse getReviewsByTourId(UUID tourId, ReviewFilterRequest filter);

    ReviewResponse createReview(CreateReviewRequest request, CustomUserDetails userDetails);

    ReviewResponse updateReviewStatus(UUID reviewId, UpdateReviewStatusRequest request);
}
