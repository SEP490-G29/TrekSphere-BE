package com.sep.treksphere.repository;

import com.sep.treksphere.entity.Tour;
import com.sep.treksphere.enums.tour.DifficultyLevel;
import com.sep.treksphere.enums.tour.TourStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TourRepository extends JpaRepository<Tour, UUID> {

     Page<Tour> findByStatusAndIsDeletedFalse(TourStatus status, Pageable pageable);

     @Query("""
               SELECT t FROM Tour t
               WHERE t.isDeleted = false
                 AND t.status = :status
                 AND (CAST(:keyword AS string) IS NULL
                      OR LOWER(t.tourName) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%'))
                      OR LOWER(t.location) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')))
                 AND (CAST(:location AS string) IS NULL
                      OR LOWER(t.location) LIKE LOWER(CONCAT('%', CAST(:location AS string), '%')))
                 AND (:difficulty IS NULL OR t.difficulty = :difficulty)
               """)
     Page<Tour> searchTours(
               @Param("status") TourStatus status,
               @Param("keyword") String keyword,
               @Param("location") String location,
               @Param("difficulty") DifficultyLevel difficulty,
               Pageable pageable);

     @Query("""
               SELECT t FROM Tour t
               JOIN FETCH t.vendor v
               WHERE t.tourId = :tourId AND t.isDeleted = false
               """)
     Optional<Tour> findDetailById(@Param("tourId") UUID tourId);
}
