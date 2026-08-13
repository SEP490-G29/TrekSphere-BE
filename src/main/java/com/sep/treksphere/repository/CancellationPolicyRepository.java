package com.sep.treksphere.repository;

import com.sep.treksphere.entity.CancellationPolicy;
import com.sep.treksphere.entity.Vendor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CancellationPolicyRepository extends JpaRepository<CancellationPolicy, UUID> {
    List<CancellationPolicy> findByVendorAndIsActiveTrueAndIsDeletedFalseOrderByCancelBeforeDaysDesc(Vendor vendor);
    List<CancellationPolicy> findByVendorAndIsDeletedFalseOrderByCancelBeforeDaysDesc(Vendor vendor);
    boolean existsByVendorAndIsActiveTrueAndIsDeletedFalse(Vendor vendor);
    Optional<CancellationPolicy> findByCancellationPolicyIdAndIsDeletedFalse(UUID cancellationPolicyId);
    boolean existsByVendorAndCancelBeforeDaysAndIsDeletedFalse(Vendor vendor, Integer cancelBeforeDays);
    boolean existsByVendorAndCancelBeforeDaysAndCancellationPolicyIdNotAndIsDeletedFalse(Vendor vendor, Integer cancelBeforeDays, UUID cancellationPolicyId);
}
