package com.sep.treksphere.repository;

import com.sep.treksphere.entity.Vendor;
import com.sep.treksphere.enums.vendor.VendorStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface VendorRepository extends JpaRepository<Vendor, UUID> {
    
    Optional<Vendor> findByManager_Email(String email);
    
    Optional<Vendor> findByManager_UserId(UUID managerId);

    boolean existsByManager_UserIdAndIsDeletedFalse(UUID managerId);
    
    boolean existsByTaxCode(String taxCode);
    boolean existsByContactEmail(String contactEmail);
    boolean existsByContactPhone(String contactPhone);
    boolean existsByContactEmailAndVendorIdNot(String contactEmail, UUID vendorId);
    boolean existsByContactPhoneAndVendorIdNot(String contactPhone, UUID vendorId);
    
    @Query("SELECT v FROM Vendor v WHERE v.isDeleted = false " +
            "AND (:status IS NULL OR v.status = :status) " +
            "AND (CAST(:keyword AS string) IS NULL OR CAST(:keyword AS string) = '' " +
            "OR LOWER(v.companyName) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) " +
            "OR LOWER(v.contactEmail) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')))")
    Page<Vendor> findByKeywordAndStatus(
            @Param("keyword") String keyword,
            @Param("status") VendorStatus status,
            Pageable pageable
    );
}
