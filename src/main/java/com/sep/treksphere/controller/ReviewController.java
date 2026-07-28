package com.sep.treksphere.controller;

import com.sep.treksphere.constant.MessageConstant;
import com.sep.treksphere.dto.request.CreateReviewRequest;
import com.sep.treksphere.dto.request.ReviewFilterRequest;
import com.sep.treksphere.dto.request.UpdateReviewStatusRequest;
import com.sep.treksphere.dto.response.ApiResponse;
import com.sep.treksphere.dto.response.ReviewResponse;
import com.sep.treksphere.dto.response.ReviewSummaryResponse;
import com.sep.treksphere.security.CustomUserDetails;
import com.sep.treksphere.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Review", description = "Các API dành cho đánh giá Tour")
public class ReviewController {

    private final ReviewService reviewService;

    // ======================== GET REVIEWS ========================

    @Operation(
        summary = "Xem đánh giá tour",
        description = "Lấy danh sách đánh giá (Rating + Content) của Tour. " +
                "Bao gồm thống kê tổng hợp (điểm trung bình, phân bổ rating) và danh sách reviews có phân trang. " +
                "Public, không cần đăng nhập."
    )
    @GetMapping("/api/v1/tours/{tourId}/reviews")
    public ResponseEntity<ApiResponse<ReviewSummaryResponse>> getReviewsByTourId(
            @Parameter(description = "UUID của tour") @PathVariable UUID tourId,
            @Valid @ParameterObject @ModelAttribute ReviewFilterRequest filter) {
        ReviewSummaryResponse result = reviewService.getReviewsByTourId(tourId, filter);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, result));
    }

    // ======================== CREATE REVIEW ========================

    @Operation(
        summary = "Viết đánh giá tour",
        description = "Trekker viết đánh giá cho tour đã hoàn thành. " +
                "Chỉ cho phép khi đơn đặt tour ở trạng thái COMPLETED. " +
                "Mỗi booking chỉ được đánh giá một lần."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('TREKKER')")
    @PostMapping("/api/v1/reviews")
    public ResponseEntity<ApiResponse<ReviewResponse>> createReview(
            @Valid @RequestBody CreateReviewRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        ReviewResponse result = reviewService.createReview(request, userDetails);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED, result, MessageConstant.REVIEW_CREATED_SUCCESSFULLY));
    }

    // ======================== UPDATE REVIEW STATUS ========================

    @Operation(
        summary = "Duyệt/Ẩn đánh giá",
        description = "Admin duyệt hoặc ẩn đánh giá vi phạm quy chuẩn cộng đồng. " +
                "Trạng thái hợp lệ: APPROVED (duyệt) hoặc HIDDEN (ẩn)."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/api/v1/reviews/{reviewId}/status")
    public ResponseEntity<ApiResponse<ReviewResponse>> updateReviewStatus(
            @Parameter(description = "UUID của đánh giá") @PathVariable UUID reviewId,
            @Valid @RequestBody UpdateReviewStatusRequest request) {
        ReviewResponse result = reviewService.updateReviewStatus(reviewId, request);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, result, MessageConstant.REVIEW_STATUS_UPDATED_SUCCESSFULLY));
    }
}
