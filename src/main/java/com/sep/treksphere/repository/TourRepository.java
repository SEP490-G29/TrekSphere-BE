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
      * Dành cho VENDOR_MANAGER: thấy PENDING_APPROVAL, APPROVED, HIDDEN, REJECTED
      * Không thấy DRAFT của staff
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
     Page<Tour> findByVendorIdForManager(
               @Param("vendorId") UUID vendorId,
               @Param("statuses") java.util.List<TourStatus> statuses,
               @Param("keyword") String keyword,
               Pageable pageable);

     /**
      * Dành cho VENDOR_STAFF:
      * - Thấy DRAFT và REJECTED của chính mình
      * - Thấy APPROVED và HIDDEN của Vendor (để xem và tạo Schedule)
      */
     @Query("""
               SELECT t FROM Tour t
               WHERE t.isDeleted = false
                 AND t.vendor.vendorId = :vendorId
                 AND (
                     (t.creator.userId = :creatorId AND t.status IN (com.sep.treksphere.enums.tour.TourStatus.DRAFT,
                                                                      com.sep.treksphere.enums.tour.TourStatus.REJECTED))
                     OR t.status IN (com.sep.treksphere.enums.tour.TourStatus.APPROVED,
                                     com.sep.treksphere.enums.tour.TourStatus.HIDDEN)
                 )
                 AND (CAST(:keyword AS string) IS NULL
                      OR LOWER(t.tourName) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%'))
                      OR LOWER(t.location) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')))
               """)
     Page<Tour> findByVendorIdForStaff(
               @Param("vendorId") UUID vendorId,
               @Param("creatorId") UUID creatorId,
               @Param("keyword") String keyword,
               Pageable pageable);
}

