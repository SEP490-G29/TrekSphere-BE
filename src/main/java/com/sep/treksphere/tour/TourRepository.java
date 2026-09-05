package com.sep.treksphere.tour;

import com.sep.treksphere.tour.schedule.TourSchedule;
import com.sep.treksphere.vendor.Vendor;
import com.sep.treksphere.tour.Tour;
import com.sep.treksphere.tour.DifficultyLevel;
import com.sep.treksphere.tour.TourStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TourRepository extends JpaRepository<Tour, UUID> {

     Page<Tour> findByStatusAndIsDeletedFalse(TourStatus status, Pageable pageable);

     @Query("""
               SELECT t FROM Tour t
               JOIN t.vendor v
               WHERE t.isDeleted = false
                 AND t.status = :status
                 AND v.status = com.sep.treksphere.vendor.VendorStatus.ACTIVE
                 AND v.isDeleted = false
                 AND (CAST(:vendorId AS uuid) IS NULL OR v.vendorId = :vendorId)
                 AND (CAST(:keyword AS string) IS NULL
                      OR LOWER(t.tourName) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%'))
                      OR LOWER(t.location) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')))
                 AND (CAST(:location AS string) IS NULL
                      OR LOWER(t.location) LIKE LOWER(CONCAT('%', CAST(:location AS string), '%')))
                 AND (CAST(:difficulty AS string) IS NULL OR t.difficulty = :difficulty)
                 AND ((CAST(:departureDate AS date) IS NULL AND CAST(:returnDate AS date) IS NULL)
                      OR EXISTS (
                          SELECT ts.tourScheduleId FROM TourSchedule ts
                          WHERE ts.tour = t
                            AND ts.isDeleted = false
                            AND ts.status = com.sep.treksphere.tour.schedule.ScheduleStatus.OPEN
                            AND (CAST(:departureDate AS date) IS NULL OR ts.departureDate = :departureDate)
                            AND (CAST(:returnDate AS date) IS NULL OR ts.returnDate = :returnDate)
                      ))
               """)
     Page<Tour> searchTours(
               @Param("status") TourStatus status,
               @Param("keyword") String keyword,
               @Param("location") String location,
               @Param("difficulty") DifficultyLevel difficulty,
               @Param("departureDate") LocalDate departureDate,
               @Param("returnDate") LocalDate returnDate,
               @Param("vendorId") UUID vendorId,
               Pageable pageable);

     @Query("""
               SELECT t FROM Tour t
               JOIN FETCH t.vendor v
               WHERE t.tourId = :tourId
                 AND t.isDeleted = false
                 AND t.status = com.sep.treksphere.tour.TourStatus.PUBLISHED
                 AND v.status = com.sep.treksphere.vendor.VendorStatus.ACTIVE
                 AND v.isDeleted = false
               """)
     Optional<Tour> findPublishedDetailById(@Param("tourId") UUID tourId);

     long countByVendorVendorIdAndStatusAndIsDeletedFalse(UUID vendorId, TourStatus status);

     @Query("""
               SELECT t FROM Tour t
               WHERE t.isDeleted = false
                 AND t.vendor.vendorId = :vendorId
                 AND (CAST(:keyword AS string) IS NULL 
                      OR LOWER(t.tourName) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%'))
                      OR LOWER(t.location) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')))
               """)
     Page<Tour> findByVendorIdAndKeyword(
               @Param("vendorId") UUID vendorId, 
               @Param("keyword") String keyword,
               Pageable pageable);

     Optional<Tour> findByTourIdAndIsDeletedFalse(UUID tourId);

     /**
      * Tìm Tour đã bị xóa mềm — phục vụ API restore
      */
     Optional<Tour> findByTourIdAndIsDeletedTrue(UUID tourId);

     /**
      * Dành cho Vendor Owner: thấy toàn bộ Tour của Vendor mình, mọi status
      * (không còn phân biệt Manager/Staff — chỉ còn một actor Vendor Owner).
      */
     @Query("""
               SELECT t FROM Tour t
               WHERE t.isDeleted = false
                 AND t.vendor.vendorId = :vendorId
                 AND t.status IN :statuses
                 AND (CAST(:keyword AS string) IS NULL
                      OR LOWER(t.tourName) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%'))
                      OR LOWER(t.location) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')))
               """)
     Page<Tour> findByVendorIdForOwner(
               @Param("vendorId") UUID vendorId,
               @Param("statuses") java.util.List<TourStatus> statuses,
               @Param("keyword") String keyword,
               Pageable pageable);

     @Query(value = """
               SELECT t.*
               FROM tour t
               JOIN vendor v ON v.vendor_id = t.vendor_id
               WHERE t.is_deleted = FALSE
                 AND t.status = 'PUBLISHED'
                 AND v.is_deleted = FALSE
                 AND v.status = 'ACTIVE'
                 AND EXISTS (
                     SELECT 1 FROM tour_schedule ts
                     WHERE ts.tour_id = t.tour_id
                       AND ts.is_deleted = FALSE
                       AND ts.status = 'OPEN'
                       AND ts.departure_date > CURRENT_DATE
                 )
                 AND (
                     CAST(:maxDifficultyRank AS integer) IS NULL
                     OR CASE t.difficulty
                          WHEN 'EASY' THEN 0 WHEN 'MODERATE' THEN 1
                          WHEN 'HARD' THEN 2 WHEN 'EXPERT' THEN 3
                        END <= :maxDifficultyRank
                 )
               ORDER BY (
                   CASE WHEN :usePreferences = TRUE AND EXISTS (
                       SELECT 1
                       FROM jsonb_array_elements_text(CAST(:areasJson AS jsonb)) area
                       WHERE LOWER(t.location) LIKE CONCAT('%', LOWER(area), '%')
                   ) THEN 60 ELSE 0 END
                   + CASE WHEN CAST(:preferredDifficulty AS varchar) IS NOT NULL
                               AND t.difficulty = :preferredDifficulty THEN 30 ELSE 0 END
                   + CASE WHEN CAST(:maxDifficultyRank AS integer) IS NOT NULL THEN 10 ELSE 0 END
               ) DESC,
               (SELECT COUNT(*) FROM matching_group mg
                 WHERE mg.tour_id = t.tour_id
                   AND mg.is_deleted = FALSE
                   AND mg.status IN ('OPEN','FULL','CLOSED','IN_PROGRESS','COMPLETED')) DESC,
               t.published_at DESC NULLS LAST,
               t.tour_id
               """,
               countQuery = """
               SELECT COUNT(*)
               FROM tour t
               JOIN vendor v ON v.vendor_id = t.vendor_id
               WHERE t.is_deleted = FALSE
                 AND t.status = 'PUBLISHED'
                 AND v.is_deleted = FALSE
                 AND v.status = 'ACTIVE'
                 AND EXISTS (
                     SELECT 1 FROM tour_schedule ts
                     WHERE ts.tour_id = t.tour_id
                       AND ts.is_deleted = FALSE
                       AND ts.status = 'OPEN'
                       AND ts.departure_date > CURRENT_DATE
                 )
                 AND (
                     CAST(:maxDifficultyRank AS integer) IS NULL
                     OR CASE t.difficulty
                          WHEN 'EASY' THEN 0 WHEN 'MODERATE' THEN 1
                          WHEN 'HARD' THEN 2 WHEN 'EXPERT' THEN 3
                        END <= :maxDifficultyRank
                 )
               """,
               nativeQuery = true)
     Page<Tour> findRecommendedTours(
               @Param("areasJson") String areasJson,
               @Param("preferredDifficulty") String preferredDifficulty,
               @Param("maxDifficultyRank") Integer maxDifficultyRank,
               @Param("usePreferences") boolean usePreferences,
               Pageable pageable);
}

