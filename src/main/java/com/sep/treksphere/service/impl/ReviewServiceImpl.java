package com.sep.treksphere.service.impl;

import com.sep.treksphere.dto.request.CreateReviewRequest;
import com.sep.treksphere.dto.request.ReviewFilterRequest;
import com.sep.treksphere.dto.request.UpdateReviewStatusRequest;
import com.sep.treksphere.dto.response.PaginationResponse;
import com.sep.treksphere.dto.response.ReviewResponse;
import com.sep.treksphere.dto.response.ReviewSummaryResponse;
import com.sep.treksphere.entity.Booking;
import com.sep.treksphere.entity.Review;
import com.sep.treksphere.entity.Tour;
import com.sep.treksphere.entity.User;
import com.sep.treksphere.enums.blog.ReviewStatus;
import com.sep.treksphere.enums.booking.BookingStatus;
import com.sep.treksphere.exception.AppException;
import com.sep.treksphere.exception.ErrorCode;
import com.sep.treksphere.repository.BookingRepository;
import com.sep.treksphere.repository.ReviewRepository;
import com.sep.treksphere.repository.TourRepository;
import com.sep.treksphere.security.CustomUserDetails;
import com.sep.treksphere.service.ReviewService;
import com.sep.treksphere.utils.PaginationUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final TourRepository tourRepository;
    private final BookingRepository bookingRepository;

    @Override
    @Transactional(readOnly = true)
    public ReviewSummaryResponse getReviewsByTourId(UUID tourId, ReviewFilterRequest filter) {
        Tour tour = tourRepository.findByTourIdAndIsDeletedFalse(tourId)
                .orElseThrow(() -> new AppException(ErrorCode.TOUR_NOT_FOUND));

        ReviewStatus approvedStatus = ReviewStatus.APPROVED;

        // Tính thống kê tổng hợp
        Double averageRating = reviewRepository.findAverageRatingByTourAndStatus(tour, approvedStatus);
        int totalReviews = reviewRepository.countByTourAndStatusAndIsDeletedFalse(tour, approvedStatus);

        // Tính phân bổ rating
        int fiveStar = reviewRepository.countByTourAndRatingAndStatusAndIsDeletedFalse(tour, 5, approvedStatus);
        int fourStar = reviewRepository.countByTourAndRatingAndStatusAndIsDeletedFalse(tour, 4, approvedStatus);
        int threeStar = reviewRepository.countByTourAndRatingAndStatusAndIsDeletedFalse(tour, 3, approvedStatus);
        int twoStar = reviewRepository.countByTourAndRatingAndStatusAndIsDeletedFalse(tour, 2, approvedStatus);
        int oneStar = reviewRepository.countByTourAndRatingAndStatusAndIsDeletedFalse(tour, 1, approvedStatus);

        // Query reviews với phân trang, lọc theo rating nếu có
        Page<Review> reviewPage;
        if (filter.getRating() != null) {
            reviewPage = reviewRepository.findByTourAndRatingAndStatusAndIsDeletedFalse(
                    tour, filter.getRating(), approvedStatus, filter.getPageable());
        } else {
            reviewPage = reviewRepository.findByTourAndStatusAndIsDeletedFalse(
                    tour, approvedStatus, filter.getPageable());
        }

        PaginationResponse<ReviewResponse> paginatedReviews =
                PaginationUtils.toPaginationResponse(reviewPage.map(this::toReviewResponse));

        return ReviewSummaryResponse.builder()
                .averageRating(averageRating)
                .totalReviews(totalReviews)
                .fiveStar(fiveStar)
                .fourStar(fourStar)
                .threeStar(threeStar)
                .twoStar(twoStar)
                .oneStar(oneStar)
                .reviews(paginatedReviews)
                .build();
    }

    @Override
    @Transactional
    public ReviewResponse createReview(CreateReviewRequest request, CustomUserDetails userDetails) {
        User currentUser = userDetails.getUser();

        // Tìm booking
        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));

        // Kiểm tra booking thuộc user hiện tại
        if (!booking.getUser().getUserId().equals(currentUser.getUserId())) {
            throw new AppException(ErrorCode.REVIEW_BOOKING_NOT_OWNED);
        }

        // Kiểm tra booking đã COMPLETED
        if (booking.getBookingStatus() != BookingStatus.COMPLETED) {
            throw new AppException(ErrorCode.REVIEW_BOOKING_NOT_COMPLETED);
        }

        // Kiểm tra chưa có review cho booking này
        if (reviewRepository.existsByBookingAndIsDeletedFalse(booking)) {
            throw new AppException(ErrorCode.REVIEW_ALREADY_EXISTS);
        }

        // Tạo review
        Review review = new Review();
        review.setTour(booking.getSchedule().getTour());
        review.setUser(currentUser);
        review.setBooking(booking);
        review.setRating(request.getRating());
        review.setContent(request.getContent());
        review.setStatus(ReviewStatus.PENDING);

        review = reviewRepository.save(review);

        return toReviewResponse(review);
    }

    @Override
    @Transactional
    public ReviewResponse updateReviewStatus(UUID reviewId, UpdateReviewStatusRequest request) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new AppException(ErrorCode.REVIEW_NOT_FOUND));

        // Validate status phải là APPROVED hoặc HIDDEN
        if (request.getStatus() != ReviewStatus.APPROVED && request.getStatus() != ReviewStatus.HIDDEN) {
            throw new AppException(ErrorCode.REVIEW_INVALID_STATUS);
        }

        review.setStatus(request.getStatus());
        review = reviewRepository.save(review);

        return toReviewResponse(review);
    }

    private ReviewResponse toReviewResponse(Review review) {
        Tour tour = review.getTour();
        User user = review.getUser();
        Booking booking = review.getBooking();

        return ReviewResponse.builder()
                .reviewId(review.getReviewId().toString())
                .rating(review.getRating())
                .content(review.getContent())
                .status(review.getStatus())
                // Thông tin người đánh giá
                .userId(user.getUserId().toString())
                .userFullName(user.getFullName())
                .userAvatarUrl(user.getAvatarUrl())
                // Thông tin tour
                .tourId(tour.getTourId().toString())
                .tourName(tour.getTourName())
                .tourCoverImageUrl(tour.getCoverImageUrl())
                // Thông tin booking
                .bookingId(booking.getBookingId().toString())
                .bookingCode(booking.getBookingCode())
                // Timestamps
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }
}
