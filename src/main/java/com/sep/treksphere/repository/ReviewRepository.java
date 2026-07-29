package com.sep.treksphere.repository;

import com.sep.treksphere.entity.Booking;
import com.sep.treksphere.entity.Review;
import com.sep.treksphere.entity.Tour;
import com.sep.treksphere.enums.blog.ReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID> {

    @Query("""
            SELECT AVG(r.rating) FROM Review r
            WHERE r.tour = :tour AND r.status = :status AND r.isDeleted = false
            """)
    Double findAverageRatingByTourAndStatus(@Param("tour") Tour tour, @Param("status") ReviewStatus status);

    int countByTourAndStatusAndIsDeletedFalse(Tour tour, ReviewStatus status);

    // Phân trang reviews theo tour và status
    Page<Review> findByTourAndStatusAndIsDeletedFalse(Tour tour, ReviewStatus status, Pageable pageable);

    // Phân trang + lọc theo rating
    @Query("SELECT r FROM Review r WHERE r.tour = :tour AND r.status = :status AND r.rating = :rating AND r.isDeleted = false")
    Page<Review> findByTourAndRatingAndStatusAndIsDeletedFalse(
            @Param("tour") Tour tour,
            @Param("rating") Integer rating,
            @Param("status") ReviewStatus status,
            Pageable pageable);

    // Kiểm tra booking đã review chưa
    boolean existsByBookingAndIsDeletedFalse(Booking booking);

    // Đếm theo từng rating (cho rating distribution)
    int countByTourAndRatingAndStatusAndIsDeletedFalse(Tour tour, Integer rating, ReviewStatus status);
}