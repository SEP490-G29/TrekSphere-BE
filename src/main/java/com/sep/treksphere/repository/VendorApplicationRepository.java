package com.sep.treksphere.repository;

import com.sep.treksphere.entity.VendorApplication;
import com.sep.treksphere.enums.vendor.ApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface VendorApplicationRepository extends JpaRepository<VendorApplication, UUID> {
    boolean existsByTaxCode(String taxCode);
    boolean existsByApplicant_UserIdAndApplicationStatus(UUID applicantId, ApplicationStatus applicationStatus);
    boolean existsByContactEmail(String contactEmail);
    boolean existsByContactPhone(String contactPhone);
    boolean existsByTaxCodeAndVendorApplicationIdNot(String taxCode, UUID vendorApplicationId);
    boolean existsByContactEmailAndVendorApplicationIdNot(String contactEmail, UUID vendorApplicationId);
    boolean existsByContactPhoneAndVendorApplicationIdNot(String contactPhone, UUID vendorApplicationId);

    @Query("SELECT va FROM VendorApplication va WHERE va.isDeleted = false " +
           "AND (:status IS NULL OR va.applicationStatus = :status) " +
           "AND (:keyword IS NULL OR :keyword = '' OR " +
           "LOWER(va.companyName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(va.taxCode) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(va.contactEmail) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(va.contactPhone) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<VendorApplication> findAllApplicationsWithFilter(
            @Param("status") ApplicationStatus status,
            @Param("keyword") String keyword,
            Pageable pageable);
}
