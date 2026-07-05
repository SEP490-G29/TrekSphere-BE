package com.sep.treksphere.repository;

import com.sep.treksphere.entity.Review;
import com.sep.treksphere.entity.Tour;
import com.sep.treksphere.enums.blog.ReviewStatus;
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
}