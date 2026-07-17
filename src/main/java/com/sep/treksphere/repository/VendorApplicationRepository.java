package com.sep.treksphere.repository;

import com.sep.treksphere.entity.VendorApplication;
import com.sep.treksphere.enums.vendor.ApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface VendorApplicationRepository extends JpaRepository<VendorApplication, UUID> {
    boolean existsByTaxCode(String taxCode);
    boolean existsByApplicant_UserIdAndApplicationStatus(UUID applicantId, ApplicationStatus applicationStatus);
    boolean existsByContactEmail(String contactEmail);
    boolean existsByContactPhone(String contactPhone);
}
