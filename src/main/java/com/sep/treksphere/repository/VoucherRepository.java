package com.sep.treksphere.repository;

import com.sep.treksphere.entity.Voucher;
import com.sep.treksphere.enums.voucher.DiscountType;
import com.sep.treksphere.enums.voucher.VoucherStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;

@Repository
public interface VoucherRepository extends JpaRepository<Voucher, UUID> {
    Optional<Voucher> findByCodeAndIsDeletedFalse(String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from Voucher v join fetch v.vendor where v.code = :code and v.isDeleted = false")
    Optional<Voucher> findByCodeForUpdate(@Param("code") String code);
    boolean existsByCodeAndIsDeletedFalse(String code);

    Page<Voucher> findByVendor_VendorIdAndStatusAndIsDeletedFalse(UUID vendorId, VoucherStatus status, Pageable pageable);

    Page<Voucher> findByVendor_VendorIdAndIsDeletedFalse(UUID vendorId, Pageable pageable);

    Optional<Voucher> findByVoucherIdAndVendor_VendorIdAndIsDeletedFalse(UUID voucherId, UUID vendorId);

    Optional<Voucher> findByVoucherIdAndVendor_VendorId(UUID voucherId, UUID vendorId);

    @Query("""
            SELECT v FROM Voucher v
            WHERE v.vendor.vendorId = :vendorId
              AND (CAST(:keyword AS string) IS NULL 
                   OR LOWER(v.code) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')))
              AND (CAST(:discountType AS string) IS NULL OR v.discountType = :discountType)
              AND (CAST(:status AS string) IS NULL OR v.status = :status)
              AND (CAST(:validUntilStart AS timestamp) IS NULL OR v.validUntil >= :validUntilStart)
              AND (CAST(:validUntilEnd AS timestamp) IS NULL OR v.validUntil <= :validUntilEnd)
              AND (CAST(:maxUsage AS integer) IS NULL OR v.maxUsage = :maxUsage)
            """)
    Page<Voucher> filterVendorVouchers(
            @Param("vendorId") UUID vendorId, 
            @Param("keyword") String keyword,
            @Param("discountType") DiscountType discountType,
            @Param("status") VoucherStatus status,
            @Param("validUntilStart") LocalDateTime validUntilStart,
            @Param("validUntilEnd") LocalDateTime validUntilEnd,
            @Param("maxUsage") Integer maxUsage,
            Pageable pageable);
}
