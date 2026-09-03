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
               WHERE t.isDeleted = false
                 AND t.status = :status
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
               Pageable pageable);

     @Query("""
               SELECT t FROM Tour t
               JOIN FETCH t.vendor v
               WHERE t.tourId = :tourId AND t.isDeleted = false
               """)
     Optional<Tour> findDetailById(@Param("tourId") UUID tourId);

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
}

