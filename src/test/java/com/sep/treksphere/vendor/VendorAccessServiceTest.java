package com.sep.treksphere.vendor;

import com.sep.treksphere.common.exception.AppException;
import com.sep.treksphere.common.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/** Bao phủ 3 method phụ của `VendorAccessService`: resolveByManagerEmail, resolveActiveByManagerEmail, requireActive. */
@ExtendWith(MockitoExtension.class)
class VendorAccessServiceTest {

    @Mock
    private VendorRepository vendorRepository;

    @InjectMocks
    private VendorAccessService vendorAccessService;

    private static final String MANAGER_EMAIL = "manager@example.com";

    private Vendor vendor() {
        Vendor vendor = new Vendor();
        vendor.setVendorId(UUID.randomUUID());
        vendor.setStatus(VendorStatus.ACTIVE);
        return vendor;
    }

    @Test
    @DisplayName("resolveByManagerEmail: email khớp vendor manager -> trả về vendor")
    void resolveByManagerEmail_Success() {
        Vendor vendor = vendor();
        when(vendorRepository.findByManager_EmailAndIsDeletedFalse(MANAGER_EMAIL)).thenReturn(Optional.of(vendor));

        Vendor result = vendorAccessService.resolveByManagerEmail(MANAGER_EMAIL);

        assertThat(result).isEqualTo(vendor);
    }

    @Test
    @DisplayName("resolveByManagerEmail: không tìm thấy vendor -> ném AppException VENDOR_NOT_FOUND")
    void resolveByManagerEmail_NotFound_ThrowsException() {
        when(vendorRepository.findByManager_EmailAndIsDeletedFalse(MANAGER_EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> vendorAccessService.resolveByManagerEmail(MANAGER_EMAIL))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.VENDOR_NOT_FOUND));
    }

    @Test
    @DisplayName("resolveActiveByManagerEmail: vendor ACTIVE -> trả về vendor")
    void resolveActiveByManagerEmail_Active_ReturnsVendor() {
        Vendor vendor = vendor();
        when(vendorRepository.findByManager_EmailAndIsDeletedFalse(MANAGER_EMAIL)).thenReturn(Optional.of(vendor));

        Vendor result = vendorAccessService.resolveActiveByManagerEmail(MANAGER_EMAIL);

        assertThat(result).isEqualTo(vendor);
    }

    @Test
    @DisplayName("resolveActiveByManagerEmail: vendor không ACTIVE -> ném AppException VENDOR_NOT_ACTIVE")
    void resolveActiveByManagerEmail_NotActive_ThrowsException() {
        Vendor vendor = vendor();
        vendor.setStatus(VendorStatus.SUSPENDED);
        when(vendorRepository.findByManager_EmailAndIsDeletedFalse(MANAGER_EMAIL)).thenReturn(Optional.of(vendor));

        assertThatThrownBy(() -> vendorAccessService.resolveActiveByManagerEmail(MANAGER_EMAIL))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.VENDOR_NOT_ACTIVE));
    }

    @Test
    @DisplayName("requireActive: vendor ACTIVE -> không ném lỗi")
    void requireActive_Active_DoesNotThrow() {
        assertThatCode(() -> vendorAccessService.requireActive(vendor())).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("requireActive: vendor không ACTIVE -> ném AppException VENDOR_NOT_ACTIVE")
    void requireActive_NotActive_ThrowsException() {
        Vendor vendor = vendor();
        vendor.setStatus(VendorStatus.PENDING);

        assertThatThrownBy(() -> vendorAccessService.requireActive(vendor))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.VENDOR_NOT_ACTIVE));
    }
}
