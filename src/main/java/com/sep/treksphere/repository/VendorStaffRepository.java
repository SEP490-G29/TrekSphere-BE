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
           "AND (:keyword IS NULL OR :keyword = '' " +
           "OR LOWER(vs.user.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(vs.user.email) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<VendorStaff> findByVendorIdAndKeyword(@Param("vendorId") UUID vendorId, @Param("keyword") String keyword, Pageable pageable);
}
