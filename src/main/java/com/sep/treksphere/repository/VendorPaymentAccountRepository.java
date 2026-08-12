package com.sep.treksphere.repository;

import com.sep.treksphere.entity.VendorPaymentAccount;
import com.sep.treksphere.enums.booking.PaymentAccountStatus;
import com.sep.treksphere.enums.booking.PaymentProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface VendorPaymentAccountRepository extends JpaRepository<VendorPaymentAccount, UUID> {
    boolean existsByVendor_VendorIdAndProviderAndOnboardingStatusAndIsDefaultTrueAndIsDeletedFalse(
            UUID vendorId, PaymentProvider provider, PaymentAccountStatus onboardingStatus);

    Optional<VendorPaymentAccount> findByVendor_VendorIdAndProviderAndOnboardingStatusAndIsDefaultTrueAndIsDeletedFalse(
            UUID vendorId, PaymentProvider provider, PaymentAccountStatus onboardingStatus);

    Optional<VendorPaymentAccount> findByProviderAndProviderChannelIdAndOnboardingStatusAndIsDeletedFalse(
            PaymentProvider provider, String providerChannelId, PaymentAccountStatus onboardingStatus);

    Optional<VendorPaymentAccount> findByProviderAndProviderChannelIdAndIsDeletedFalse(
            PaymentProvider provider, String providerChannelId);

    Optional<VendorPaymentAccount> findByVendor_VendorIdAndProviderAndIsDefaultTrueAndIsDeletedFalse(
            UUID vendorId, PaymentProvider provider);
}
