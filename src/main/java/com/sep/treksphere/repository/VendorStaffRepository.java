package com.sep.treksphere.repository;

import com.sep.treksphere.entity.VendorStaff;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VendorStaffRepository extends JpaRepository<VendorStaff, UUID> {

    Optional<VendorStaff> findByUser_UserIdAndIsActiveTrueAndIsDeletedFalse(UUID userId);
    Optional<VendorStaff> findByUser_UserId(UUID userId);

    @Query("SELECT vs FROM VendorStaff vs WHERE vs.vendor.vendorId = :vendorId " +
           "AND (CAST(:keyword AS string) IS NULL OR CAST(:keyword AS string) = '' " +
           "OR LOWER(vs.user.fullName) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) " +
           "OR LOWER(vs.user.email) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')))")
    Page<VendorStaff> findByVendorIdAndKeyword(@Param("vendorId") UUID vendorId, @Param("keyword") String keyword, Pageable pageable);

    @Query(value = "SELECT DISTINCT vs FROM VendorStaff vs JOIN vs.user.roles r " +
           "WHERE vs.vendor.vendorId = :vendorId " +
           "AND vs.isActive = true " +
           "AND vs.isDeleted = false " +
           "AND vs.user.isDeleted = false " +
           "AND r.roleName = 'COORDINATOR' " +
           "AND (CAST(:keyword AS string) IS NULL OR CAST(:keyword AS string) = '' " +
           "OR LOWER(vs.user.fullName) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) " +
           "OR LOWER(vs.user.email) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')))",
           countQuery = "SELECT COUNT(DISTINCT vs) FROM VendorStaff vs JOIN vs.user.roles r " +
           "WHERE vs.vendor.vendorId = :vendorId " +
           "AND vs.isActive = true " +
           "AND vs.isDeleted = false " +
           "AND vs.user.isDeleted = false " +
           "AND r.roleName = 'COORDINATOR' " +
           "AND (CAST(:keyword AS string) IS NULL OR CAST(:keyword AS string) = '' " +
           "OR LOWER(vs.user.fullName) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) " +
           "OR LOWER(vs.user.email) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')))")
    Page<VendorStaff> findActiveCoordinatorsByVendorIdAndKeyword(
            @Param("vendorId") UUID vendorId,
            @Param("keyword") String keyword,
            Pageable pageable);

    Optional<VendorStaff> findByUser_Email(String email);
    Optional<VendorStaff> findByUser_EmailAndIsActiveTrueAndIsDeletedFalse(String email);
}

