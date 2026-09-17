package com.sep.treksphere.vendor;

import com.sep.treksphere.common.exception.AppException;
import com.sep.treksphere.common.exception.ErrorCode;
import com.sep.treksphere.common.security.CustomUserDetails;
import com.sep.treksphere.file.FileService;
import com.sep.treksphere.notification.NotificationService;
import com.sep.treksphere.tour.TourRepository;
import com.sep.treksphere.tour.TourStatus;
import com.sep.treksphere.user.User;
import com.sep.treksphere.vendor.application.VendorApplicationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Bao phủ 5 method mà `unit_test_gap_report.md` liệt kê thiếu cho `VendorService`: getVendors,
 * getVendorProfile, updateVendorProfile, updateVendorStatus, getPublicVendorProfile.
 */
@ExtendWith(MockitoExtension.class)
class VendorServiceTest {

    @Mock private VendorRepository vendorRepository;
    @Mock private VendorApplicationRepository vendorApplicationRepository;
    @Mock private FileService fileService;
    @Mock private VendorMapper vendorMapper;
    @Mock private NotificationService notificationService;
    @Mock private VendorAccessService vendorAccessService;
    @Mock private TourRepository tourRepository;

    @InjectMocks
    private VendorService vendorService;

    private static final String MANAGER_EMAIL = "manager@example.com";

    private Vendor vendor;
    private User manager;

    @BeforeEach
    void setUp() {
        manager = new User();
        manager.setUserId(UUID.randomUUID());
        manager.setEmail(MANAGER_EMAIL);

        vendor = new Vendor();
        vendor.setVendorId(UUID.randomUUID());
        vendor.setManager(manager);
        vendor.setCompanyName("Trek Co");
        vendor.setContactEmail("old@trekco.com");
        vendor.setContactPhone("0900000000");
        vendor.setStatus(VendorStatus.ACTIVE);
    }

    // ---------------------------------------------------------------
    // getVendors
    // ---------------------------------------------------------------

    @Test
    @DisplayName("getVendors: trả về danh sách vendor đã phân trang cho Admin")
    void getVendors_Success() {
        VendorFilterRequest request = new VendorFilterRequest();
        Page<Vendor> page = new PageImpl<>(List.of(vendor));
        when(vendorRepository.findByKeywordAndStatus(eq(request.getKeyword()), eq(request.getStatus()), any()))
                .thenReturn(page);
        when(vendorMapper.toVendorResponse(vendor)).thenReturn(new VendorResponse());

        var result = vendorService.getVendors(request);

        assertThat(result.getContent()).hasSize(1);
    }

    // ---------------------------------------------------------------
    // getVendorProfile
    // ---------------------------------------------------------------

    @Test
    @DisplayName("getVendorProfile: trả về hồ sơ của đúng vendor đang đăng nhập")
    void getVendorProfile_Success() {
        CustomUserDetails userDetails = new CustomUserDetails(manager);
        when(vendorAccessService.resolveByManagerEmail(MANAGER_EMAIL)).thenReturn(vendor);
        VendorProfileResponse expected = new VendorProfileResponse();
        when(vendorMapper.toVendorProfileResponse(vendor)).thenReturn(expected);

        VendorProfileResponse response = vendorService.getVendorProfile(userDetails);

        assertThat(response).isEqualTo(expected);
    }

    // ---------------------------------------------------------------
    // updateVendorProfile
    // ---------------------------------------------------------------

    @Test
    @DisplayName("updateVendorProfile: hợp lệ -> cập nhật mô tả/địa chỉ/website")
    void updateVendorProfile_Success() {
        CustomUserDetails userDetails = new CustomUserDetails(manager);
        VendorProfileUpdateRequest request = new VendorProfileUpdateRequest();
        request.setDescription("Mô tả mới");
        request.setBusinessAddress("123 Sa Pa");
        request.setWebsiteUrl("https://trekco.com");

        when(vendorAccessService.resolveActiveByManagerEmail(MANAGER_EMAIL)).thenReturn(vendor);
        when(vendorRepository.save(vendor)).thenReturn(vendor);
        when(vendorMapper.toVendorProfileResponse(vendor)).thenReturn(new VendorProfileResponse());

        vendorService.updateVendorProfile(userDetails, request);

        assertThat(vendor.getDescription()).isEqualTo("Mô tả mới");
        assertThat(vendor.getBusinessAddress()).isEqualTo("123 Sa Pa");
        assertThat(vendor.getWebsiteUrl()).isEqualTo("https://trekco.com");
    }

    @Test
    @DisplayName("updateVendorProfile: email liên hệ mới đã được vendor khác dùng -> ném AppException")
    void updateVendorProfile_DuplicateContactEmail_ThrowsException() {
        CustomUserDetails userDetails = new CustomUserDetails(manager);
        VendorProfileUpdateRequest request = new VendorProfileUpdateRequest();
        request.setContactEmail("new@trekco.com");

        when(vendorAccessService.resolveActiveByManagerEmail(MANAGER_EMAIL)).thenReturn(vendor);
        when(vendorRepository.existsByContactEmailAndVendorIdNot("new@trekco.com", vendor.getVendorId()))
                .thenReturn(true);

        assertThatThrownBy(() -> vendorService.updateVendorProfile(userDetails, request))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.CONTACT_EMAIL_ALREADY_EXISTS));
    }

    @Test
    @DisplayName("updateVendorProfile: SĐT liên hệ mới đã được vendor khác dùng -> ném AppException")
    void updateVendorProfile_DuplicateContactPhone_ThrowsException() {
        CustomUserDetails userDetails = new CustomUserDetails(manager);
        VendorProfileUpdateRequest request = new VendorProfileUpdateRequest();
        request.setContactPhone("0911111111");

        when(vendorAccessService.resolveActiveByManagerEmail(MANAGER_EMAIL)).thenReturn(vendor);
        when(vendorRepository.existsByContactPhoneAndVendorIdNot("0911111111", vendor.getVendorId()))
                .thenReturn(true);

        assertThatThrownBy(() -> vendorService.updateVendorProfile(userDetails, request))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.CONTACT_PHONE_ALREADY_EXISTS));
    }

    // ---------------------------------------------------------------
    // updateVendorStatus
    // ---------------------------------------------------------------

    @Test
    @DisplayName("updateVendorStatus: chuyển hợp lệ ACTIVE -> SUSPENDED -> lưu và thông báo")
    void updateVendorStatus_ValidTransition_Success() {
        VendorStatusUpdateRequest request = new VendorStatusUpdateRequest();
        request.setStatus(VendorStatus.SUSPENDED);
        when(vendorRepository.findById(vendor.getVendorId())).thenReturn(Optional.of(vendor));
        when(vendorRepository.save(vendor)).thenReturn(vendor);
        when(vendorMapper.toVendorResponse(vendor)).thenReturn(new VendorResponse());

        vendorService.updateVendorStatus(vendor.getVendorId(), request);

        assertThat(vendor.getStatus()).isEqualTo(VendorStatus.SUSPENDED);
    }

    @Test
    @DisplayName("updateVendorStatus: chuyển không hợp lệ PENDING -> SUSPENDED -> ném AppException")
    void updateVendorStatus_InvalidTransition_ThrowsException() {
        vendor.setStatus(VendorStatus.PENDING);
        VendorStatusUpdateRequest request = new VendorStatusUpdateRequest();
        request.setStatus(VendorStatus.SUSPENDED);
        when(vendorRepository.findById(vendor.getVendorId())).thenReturn(Optional.of(vendor));

        assertThatThrownBy(() -> vendorService.updateVendorStatus(vendor.getVendorId(), request))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_VENDOR_STATUS_TRANSITION));
    }

    @Test
    @DisplayName("updateVendorStatus: vendor không tồn tại -> ném AppException VENDOR_NOT_FOUND")
    void updateVendorStatus_VendorNotFound_ThrowsException() {
        UUID randomId = UUID.randomUUID();
        VendorStatusUpdateRequest request = new VendorStatusUpdateRequest();
        request.setStatus(VendorStatus.SUSPENDED);
        when(vendorRepository.findById(randomId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> vendorService.updateVendorStatus(randomId, request))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.VENDOR_NOT_FOUND));
    }

    // ---------------------------------------------------------------
    // getPublicVendorProfile
    // ---------------------------------------------------------------

    @Test
    @DisplayName("getPublicVendorProfile: vendor ACTIVE -> trả hồ sơ công khai kèm số tour đã publish")
    void getPublicVendorProfile_Success() {
        when(vendorRepository.findByVendorIdAndStatusAndIsDeletedFalse(vendor.getVendorId(), VendorStatus.ACTIVE))
                .thenReturn(Optional.of(vendor));
        when(tourRepository.countByVendorVendorIdAndStatusAndIsDeletedFalse(vendor.getVendorId(), TourStatus.PUBLISHED))
                .thenReturn(5L);

        PublicVendorProfileResponse response = vendorService.getPublicVendorProfile(vendor.getVendorId());

        assertThat(response.getVendorId()).isEqualTo(vendor.getVendorId());
        assertThat(response.getCompanyName()).isEqualTo("Trek Co");
        assertThat(response.getPublishedTourCount()).isEqualTo(5L);
    }

    @Test
    @DisplayName("getPublicVendorProfile: vendor không ACTIVE/không tồn tại -> ném AppException VENDOR_NOT_FOUND")
    void getPublicVendorProfile_NotActiveOrNotFound_ThrowsException() {
        UUID randomId = UUID.randomUUID();
        when(vendorRepository.findByVendorIdAndStatusAndIsDeletedFalse(randomId, VendorStatus.ACTIVE))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> vendorService.getPublicVendorProfile(randomId))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.VENDOR_NOT_FOUND));
    }
}
