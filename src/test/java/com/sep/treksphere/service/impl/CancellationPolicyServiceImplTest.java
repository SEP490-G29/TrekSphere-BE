package com.sep.treksphere.service.impl;

import com.sep.treksphere.entity.CancellationPolicy;
import com.sep.treksphere.entity.Vendor;
import com.sep.treksphere.entity.VendorStaff;
import com.sep.treksphere.repository.CancellationPolicyRepository;
import com.sep.treksphere.repository.VendorRepository;
import com.sep.treksphere.repository.VendorStaffRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CancellationPolicyServiceImplTest {

    @Mock private CancellationPolicyRepository cancellationPolicyRepository;
    @Mock private VendorRepository vendorRepository;
    @Mock private VendorStaffRepository vendorStaffRepository;

    @InjectMocks private CancellationPolicyServiceImpl service;

    @Test
    void activeVendorStaffCanViewVendorCancellationPolicies() {
        String email = "staff@example.com";
        Vendor vendor = new Vendor();
        vendor.setVendorId(UUID.randomUUID());
        VendorStaff staff = new VendorStaff();
        staff.setVendor(vendor);

        CancellationPolicy policy = new CancellationPolicy();
        policy.setCancellationPolicyId(UUID.randomUUID());
        policy.setVendor(vendor);
        policy.setCancelBeforeDays(7);
        policy.setRefundPercentage(70);
        policy.setDescription("Hoàn 70% trước 7 ngày");
        policy.setIsActive(true);

        when(vendorRepository.findByManager_Email(email)).thenReturn(Optional.empty());
        when(vendorStaffRepository.findByUser_EmailAndIsActiveTrueAndIsDeletedFalse(email))
                .thenReturn(Optional.of(staff));
        when(cancellationPolicyRepository.findByVendorAndIsDeletedFalseOrderByCancelBeforeDaysDesc(vendor))
                .thenReturn(List.of(policy));

        var result = service.getVendorPolicies(email);

        assertEquals(1, result.size());
        assertEquals(70, result.getFirst().getRefundPercentage());
    }
}
