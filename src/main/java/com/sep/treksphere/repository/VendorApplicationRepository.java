package com.sep.treksphere.repository;

import com.sep.treksphere.entity.VendorApplication;
import com.sep.treksphere.enums.vendor.ApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface VendorApplicationRepository extends JpaRepository<VendorApplication, UUID> {
    Optional<VendorApplication> findByVendorApplicationIdAndIsDeletedFalse(UUID vendorApplicationId);
    boolean existsByApplicant_UserIdAndIsDeletedFalse(UUID applicantId);
    boolean existsByApplicant_UserIdAndVendorApplicationIdNotAndIsDeletedFalse(
            UUID applicantId,
            UUID vendorApplicationId
    );

    @Query("SELECT va.applicant.userId FROM VendorApplication va " +
           "WHERE va.vendorApplicationId = :applicationId AND va.isDeleted = false")
    Optional<UUID> findApplicantIdByApplicationId(@Param("applicationId") UUID applicationId);

    boolean existsByTaxCode(String taxCode);
    boolean existsByApplicant_UserIdAndApplicationStatus(UUID applicantId, ApplicationStatus applicationStatus);
    boolean existsByContactEmail(String contactEmail);
    boolean existsByContactPhone(String contactPhone);
    boolean existsByTaxCodeAndVendorApplicationIdNot(String taxCode, UUID vendorApplicationId);
    boolean existsByContactEmailAndVendorApplicationIdNot(String contactEmail, UUID vendorApplicationId);
    boolean existsByContactPhoneAndVendorApplicationIdNot(String contactPhone, UUID vendorApplicationId);
    boolean existsByContactEmailAndApplicant_UserIdNot(String contactEmail, UUID applicantId);
    boolean existsByContactPhoneAndApplicant_UserIdNot(String contactPhone, UUID applicantId);

    @Query("SELECT va FROM VendorApplication va WHERE va.isDeleted = false " +
           "AND va.applicationStatus IN :visibleStatuses " +
           "AND (CAST(:status AS string) IS NULL OR va.applicationStatus = :status) " +
           "AND (CAST(:keyword AS string) IS NULL OR CAST(:keyword AS string) = '' OR " +
           "LOWER(va.companyName) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) OR " +
           "LOWER(va.taxCode) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) OR " +
           "LOWER(va.contactEmail) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) OR " +
           "LOWER(va.contactPhone) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')))")
    Page<VendorApplication> findAllApplicationsWithFilter(
            @Param("visibleStatuses") Set<ApplicationStatus> visibleStatuses,
            @Param("status") ApplicationStatus status,
            @Param("keyword") String keyword,
            Pageable pageable);

    @Query("SELECT va FROM VendorApplication va WHERE va.isDeleted = false " +
           "AND va.applicant.userId = :applicantId " +
           "AND (CAST(:status AS string) IS NULL OR va.applicationStatus = :status) " +
           "AND (CAST(:keyword AS string) IS NULL OR CAST(:keyword AS string) = '' OR " +
           "LOWER(va.companyName) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) OR " +
           "LOWER(va.taxCode) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) OR " +
           "LOWER(va.contactEmail) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) OR " +
           "LOWER(va.contactPhone) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%'))) ")
    Page<VendorApplication> findMyApplicationsWithFilter(
            @Param("applicantId") UUID applicantId,
            @Param("status") ApplicationStatus status,
            @Param("keyword") String keyword,
            Pageable pageable);
}
