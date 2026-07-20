package com.sep.treksphere.repository;

import com.sep.treksphere.entity.PorterProfile;
import com.sep.treksphere.enums.vendor.PorterStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface PorterProfileRepository extends JpaRepository<PorterProfile, UUID> {

    @Query("SELECT p FROM PorterProfile p WHERE p.vendor.vendorId = :vendorId " +
            "AND p.isDeleted = false " +
            "AND (LOWER(p.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR p.phone LIKE CONCAT('%', :keyword, '%')) " +
            "AND (:status IS NULL OR p.status = :status)")
    Page<PorterProfile> findByVendorIdAndFilters(
            @Param("vendorId") UUID vendorId,
            @Param("keyword") String keyword,
            @Param("status") PorterStatus status,
            Pageable pageable);

    @Query("SELECT p FROM PorterProfile p WHERE p.vendor.vendorId = :vendorId " +
            "AND p.isDeleted = false " +
            "AND (LOWER(p.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR p.phone LIKE CONCAT('%', :keyword, '%')) " +
            "AND (:status IS NULL OR p.status = :status)")
    List<PorterProfile> findAllByVendorIdAndFilters(
            @Param("vendorId") UUID vendorId,
            @Param("keyword") String keyword,
            @Param("status") PorterStatus status);
}
