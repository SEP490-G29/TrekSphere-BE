package com.sep.treksphere.vendor;

import com.sep.treksphere.common.exception.AppException;
import com.sep.treksphere.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VendorAccessService {

    private final VendorRepository vendorRepository;

    public Vendor resolveByManagerEmail(String email) {
        return vendorRepository.findByManager_EmailAndIsDeletedFalse(email)
                .orElseThrow(() -> new AppException(ErrorCode.VENDOR_NOT_FOUND));
    }

    public Vendor resolveActiveByManagerEmail(String email) {
        Vendor vendor = resolveByManagerEmail(email);
        requireActive(vendor);
        return vendor;
    }

    public void requireActive(Vendor vendor) {
        if (vendor.getStatus() != VendorStatus.ACTIVE) {
            throw new AppException(ErrorCode.VENDOR_NOT_ACTIVE);
        }
    }
}
