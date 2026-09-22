package com.sep.treksphere.tour.repository;

import com.sep.treksphere.tour.entity.Tour;
import com.sep.treksphere.tour.enums.DifficultyLevel;
import com.sep.treksphere.tour.enums.TourStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
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
                 AND v.status = com.sep.treksphere.vendor.enums.VendorStatus.ACTIVE
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
                            AND ts.status = com.sep.treksphere.tour.enums.ScheduleStatus.OPEN
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
                 AND t.status = com.sep.treksphere.tour.enums.TourStatus.PUBLISHED
                 AND v.status = com.sep.treksphere.vendor.enums.VendorStatus.ACTIVE
                 AND v.isDeleted = false
               """)
     Optional<Tour> findPublishedDetailById(@Param("tourId") UUID tourId);

     @Query("""
               SELECT t FROM Tour t
               JOIN FETCH t.vendor v
               WHERE t.tourId IN :tourIds
                 AND t.isDeleted = false
                 AND t.status = com.sep.treksphere.tour.enums.TourStatus.PUBLISHED
                 AND v.status = com.sep.treksphere.vendor.enums.VendorStatus.ACTIVE
                 AND v.isDeleted = false
               """)
     List<Tour> findPublishedByIds(@Param("tourIds") Collection<UUID> tourIds);

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

     Optional<Tour> findByTourIdAndIsDeletedTrue(UUID tourId);

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

