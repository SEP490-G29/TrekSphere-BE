package com.sep.treksphere.repository;

import com.sep.treksphere.entity.VendorPaymentAccount;
import com.sep.treksphere.enums.booking.PaymentAccountStatus;
import com.sep.treksphere.enums.booking.PaymentProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

public interface VendorPaymentAccountRepository extends JpaRepository<VendorPaymentAccount, UUID> {
    @Query("select (count(a) > 0) from VendorPaymentAccount a " +
            "where a.vendor.vendorId = :vendorId and a.provider = :provider " +
            "and a.onboardingStatus = :status and a.isDefault = true " +
            "and a.refundHold = false and a.isDeleted = false")
    boolean existsByVendor_VendorIdAndProviderAndOnboardingStatusAndIsDefaultTrueAndIsDeletedFalse(
            @Param("vendorId") UUID vendorId,
            @Param("provider") PaymentProvider provider,
            @Param("status") PaymentAccountStatus onboardingStatus);

    @Query("select a from VendorPaymentAccount a " +
            "where a.vendor.vendorId = :vendorId and a.provider = :provider " +
            "and a.onboardingStatus = :status and a.isDefault = true " +
            "and a.refundHold = false and a.isDeleted = false")
    Optional<VendorPaymentAccount> findByVendor_VendorIdAndProviderAndOnboardingStatusAndIsDefaultTrueAndIsDeletedFalse(
            @Param("vendorId") UUID vendorId,
            @Param("provider") PaymentProvider provider,
            @Param("status") PaymentAccountStatus onboardingStatus);

    Optional<VendorPaymentAccount> findByProviderAndProviderChannelIdAndOnboardingStatusAndIsDeletedFalse(
            PaymentProvider provider, String providerChannelId, PaymentAccountStatus onboardingStatus);

    Optional<VendorPaymentAccount> findByProviderAndProviderChannelIdAndIsDeletedFalse(
            PaymentProvider provider, String providerChannelId);

    Optional<VendorPaymentAccount> findByVendor_VendorIdAndProviderAndIsDefaultTrueAndIsDeletedFalse(
            UUID vendorId, PaymentProvider provider);

    List<VendorPaymentAccount> findByVendor_VendorIdAndRefundHoldTrueAndIsDeletedFalse(UUID vendorId);
}
