package com.sep.treksphere.repository;

import com.sep.treksphere.entity.VendorEquipment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface VendorEquipmentRepository extends JpaRepository<VendorEquipment, UUID> {
    @Query("SELECT v FROM VendorEquipment v WHERE v.vendor.vendorId = :vendorId " +
            "AND v.isDeleted = false " +
            "AND LOWER(v.equipmentName) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<VendorEquipment> findByVendorIdAndKeyword(
            @Param("vendorId") UUID vendorId,
            @Param("keyword") String keyword,
            Pageable pageable);

    @Query("SELECT v FROM VendorEquipment v WHERE v.vendor.vendorId = :vendorId " +
            "AND v.isDeleted = false " +
            "AND LOWER(v.equipmentName) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<VendorEquipment> findAllByVendorIdAndKeyword(
            @Param("vendorId") UUID vendorId,
            @Param("keyword") String keyword);
}
