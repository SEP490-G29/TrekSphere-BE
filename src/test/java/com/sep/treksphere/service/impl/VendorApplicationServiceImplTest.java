package com.sep.treksphere.service.impl;

import com.sep.treksphere.dto.request.VendorApplicationRequest;
import com.sep.treksphere.dto.response.VendorApplicationResponse;
import com.sep.treksphere.entity.User;
import com.sep.treksphere.entity.VendorApplication;
import com.sep.treksphere.enums.vendor.ApplicationStatus;
import com.sep.treksphere.exception.AppException;
import com.sep.treksphere.exception.ErrorCode;
import com.sep.treksphere.mapper.VendorApplicationMapper;
import com.sep.treksphere.repository.UserRepository;
import com.sep.treksphere.repository.VendorApplicationRepository;
import com.sep.treksphere.repository.VendorRepository;
import com.sep.treksphere.service.FileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VendorApplicationServiceImplTest {

    @Mock
    private VendorApplicationRepository vendorApplicationRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private VendorRepository vendorRepository;
    @Mock
    private VendorApplicationMapper vendorApplicationMapper;
    @Mock
    private FileService fileService;

    @InjectMocks
    private VendorApplicationServiceImpl vendorApplicationService;

    private UUID applicantId;
    private User applicant;
    private VendorApplicationRequest request;
    private VendorApplication vendorApplication;
    private VendorApplicationResponse response;
    private MultipartFile mockFile;

    @BeforeEach
    void setUp() {
        applicantId = UUID.randomUUID();

        applicant = new User();
        applicant.setUserId(applicantId);
        applicant.setEmail("applicant@test.com");

        mockFile = new MockMultipartFile(
                "businessLicense",
                "license.pdf",
                "application/pdf",
                "mock content".getBytes()
        );

        request = VendorApplicationRequest.builder()
                .companyName("Cong Ty TNHH Trekking Viet")
                .contactEmail("contact@trekkingviet.com")
                .contactPhone("0987654321")
                .businessDescription("Dich vu trekking chuyen nghiep")
                .taxCode("1234567890")
                .businessLicense(mockFile)
                .build();

        vendorApplication = new VendorApplication();
        vendorApplication.setVendorApplicationId(UUID.randomUUID());
        vendorApplication.setApplicant(applicant);
        vendorApplication.setCompanyName(request.getCompanyName());
        vendorApplication.setContactEmail(request.getContactEmail());
        vendorApplication.setContactPhone(request.getContactPhone());
        vendorApplication.setBusinessDescription(request.getBusinessDescription());
        vendorApplication.setTaxCode(request.getTaxCode());
        vendorApplication.setApplicationStatus(ApplicationStatus.DRAFT);
        vendorApplication.setBusinessLicenseUrl("http://mockurl.com/license.pdf");

        response = VendorApplicationResponse.builder()
                .vendorApplicationId(vendorApplication.getVendorApplicationId())
                .companyName(vendorApplication.getCompanyName())
                .contactEmail(vendorApplication.getContactEmail())
                .contactPhone(vendorApplication.getContactPhone())
                .businessDescription(vendorApplication.getBusinessDescription())
                .taxCode(vendorApplication.getTaxCode())
                .applicationStatus(ApplicationStatus.DRAFT)
                .businessLicenseUrl(vendorApplication.getBusinessLicenseUrl())
                .build();
    }

    @Test
    void saveDraftApplication_Success() {
        // Arrange
        when(userRepository.findById(applicantId)).thenReturn(Optional.of(applicant));
        when(vendorApplicationRepository.existsByApplicant_UserIdAndApplicationStatus(applicantId, ApplicationStatus.PENDING))
                .thenReturn(false);
        when(vendorApplicationRepository.existsByTaxCode(request.getTaxCode())).thenReturn(false);
        when(vendorRepository.existsByTaxCode(request.getTaxCode())).thenReturn(false);
        when(vendorApplicationRepository.existsByContactEmail(request.getContactEmail())).thenReturn(false);
        when(vendorRepository.existsByContactEmail(request.getContactEmail())).thenReturn(false);
        when(vendorApplicationRepository.existsByContactPhone(request.getContactPhone())).thenReturn(false);
        when(vendorRepository.existsByContactPhone(request.getContactPhone())).thenReturn(false);

        when(fileService.uploadFile(any(MultipartFile.class), anyString())).thenReturn("http://mockurl.com/license.pdf");
        when(vendorApplicationMapper.toEntity(request)).thenReturn(vendorApplication);
        when(vendorApplicationRepository.save(any(VendorApplication.class))).thenReturn(vendorApplication);
        when(vendorApplicationMapper.toResponse(vendorApplication)).thenReturn(response);

        // Act
        VendorApplicationResponse result = vendorApplicationService.saveDraftApplication(applicantId, request);

        // Assert
        assertNotNull(result);
        assertEquals(ApplicationStatus.DRAFT, result.getApplicationStatus());
        assertEquals("1234567890", result.getTaxCode());
        assertEquals("http://mockurl.com/license.pdf", result.getBusinessLicenseUrl());
    }

    @Test
    void saveDraftApplication_UserNotFound_ThrowsException() {
        // Arrange
        when(userRepository.findById(applicantId)).thenReturn(Optional.empty());

        // Act & Assert
        AppException exception = assertThrows(AppException.class, () ->
                vendorApplicationService.saveDraftApplication(applicantId, request)
        );
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
        assertEquals(ErrorCode.USER_NOT_FOUND.getMessage(), exception.getMessage());
    }

    @Test
    void saveDraftApplication_PendingApplicationExists_ThrowsException() {
        // Arrange
        when(userRepository.findById(applicantId)).thenReturn(Optional.of(applicant));
        when(vendorApplicationRepository.existsByApplicant_UserIdAndApplicationStatus(applicantId, ApplicationStatus.PENDING))
                .thenReturn(true);

        // Act & Assert
        AppException exception = assertThrows(AppException.class, () ->
                vendorApplicationService.saveDraftApplication(applicantId, request)
        );
        assertEquals(ErrorCode.APPLICATION_PENDING_EXISTS, exception.getErrorCode());
        assertEquals(ErrorCode.APPLICATION_PENDING_EXISTS.getMessage(), exception.getMessage());
    }

    @Test
    void saveDraftApplication_TaxCodeExistsInApplications_ThrowsException() {
        // Arrange
        when(userRepository.findById(applicantId)).thenReturn(Optional.of(applicant));
        when(vendorApplicationRepository.existsByApplicant_UserIdAndApplicationStatus(applicantId, ApplicationStatus.PENDING))
                .thenReturn(false);
        when(vendorApplicationRepository.existsByTaxCode(request.getTaxCode())).thenReturn(true);

        // Act & Assert
        AppException exception = assertThrows(AppException.class, () ->
                vendorApplicationService.saveDraftApplication(applicantId, request)
        );
        assertEquals(ErrorCode.TAX_CODE_ALREADY_EXISTS, exception.getErrorCode());
        assertEquals(ErrorCode.TAX_CODE_ALREADY_EXISTS.getMessage(), exception.getMessage());
    }

    @Test
    void saveDraftApplication_TaxCodeExistsInVendors_ThrowsException() {
        // Arrange
        when(userRepository.findById(applicantId)).thenReturn(Optional.of(applicant));
        when(vendorApplicationRepository.existsByApplicant_UserIdAndApplicationStatus(applicantId, ApplicationStatus.PENDING))
                .thenReturn(false);
        when(vendorApplicationRepository.existsByTaxCode(request.getTaxCode())).thenReturn(false);
        when(vendorRepository.existsByTaxCode(request.getTaxCode())).thenReturn(true);

        // Act & Assert
        AppException exception = assertThrows(AppException.class, () ->
                vendorApplicationService.saveDraftApplication(applicantId, request)
        );
        assertEquals(ErrorCode.TAX_CODE_ALREADY_EXISTS, exception.getErrorCode());
        assertEquals(ErrorCode.TAX_CODE_ALREADY_EXISTS.getMessage(), exception.getMessage());
    }

    @Test
    void saveDraftApplication_ContactEmailExistsInApplications_ThrowsException() {
        // Arrange
        when(userRepository.findById(applicantId)).thenReturn(Optional.of(applicant));
        when(vendorApplicationRepository.existsByApplicant_UserIdAndApplicationStatus(applicantId, ApplicationStatus.PENDING))
                .thenReturn(false);
        when(vendorApplicationRepository.existsByTaxCode(request.getTaxCode())).thenReturn(false);
        when(vendorRepository.existsByTaxCode(request.getTaxCode())).thenReturn(false);
        when(vendorApplicationRepository.existsByContactEmail(request.getContactEmail())).thenReturn(true);

        // Act & Assert
        AppException exception = assertThrows(AppException.class, () ->
                vendorApplicationService.saveDraftApplication(applicantId, request)
        );
        assertEquals(ErrorCode.CONTACT_EMAIL_ALREADY_EXISTS, exception.getErrorCode());
        assertEquals(ErrorCode.CONTACT_EMAIL_ALREADY_EXISTS.getMessage(), exception.getMessage());
    }

    @Test
    void saveDraftApplication_ContactEmailExistsInVendors_ThrowsException() {
        // Arrange
        when(userRepository.findById(applicantId)).thenReturn(Optional.of(applicant));
        when(vendorApplicationRepository.existsByApplicant_UserIdAndApplicationStatus(applicantId, ApplicationStatus.PENDING))
                .thenReturn(false);
        when(vendorApplicationRepository.existsByTaxCode(request.getTaxCode())).thenReturn(false);
        when(vendorRepository.existsByTaxCode(request.getTaxCode())).thenReturn(false);
        when(vendorApplicationRepository.existsByContactEmail(request.getContactEmail())).thenReturn(false);
        when(vendorRepository.existsByContactEmail(request.getContactEmail())).thenReturn(true);

        // Act & Assert
        AppException exception = assertThrows(AppException.class, () ->
                vendorApplicationService.saveDraftApplication(applicantId, request)
        );
        assertEquals(ErrorCode.CONTACT_EMAIL_ALREADY_EXISTS, exception.getErrorCode());
        assertEquals(ErrorCode.CONTACT_EMAIL_ALREADY_EXISTS.getMessage(), exception.getMessage());
    }

    @Test
    void saveDraftApplication_ContactPhoneExistsInApplications_ThrowsException() {
        // Arrange
        when(userRepository.findById(applicantId)).thenReturn(Optional.of(applicant));
        when(vendorApplicationRepository.existsByApplicant_UserIdAndApplicationStatus(applicantId, ApplicationStatus.PENDING))
                .thenReturn(false);
        when(vendorApplicationRepository.existsByTaxCode(request.getTaxCode())).thenReturn(false);
        when(vendorRepository.existsByTaxCode(request.getTaxCode())).thenReturn(false);
        when(vendorApplicationRepository.existsByContactEmail(request.getContactEmail())).thenReturn(false);
        when(vendorRepository.existsByContactEmail(request.getContactEmail())).thenReturn(false);
        when(vendorApplicationRepository.existsByContactPhone(request.getContactPhone())).thenReturn(true);

        // Act & Assert
        AppException exception = assertThrows(AppException.class, () ->
                vendorApplicationService.saveDraftApplication(applicantId, request)
        );
        assertEquals(ErrorCode.CONTACT_PHONE_ALREADY_EXISTS, exception.getErrorCode());
        assertEquals(ErrorCode.CONTACT_PHONE_ALREADY_EXISTS.getMessage(), exception.getMessage());
    }

    @Test
    void saveDraftApplication_ContactPhoneExistsInVendors_ThrowsException() {
        // Arrange
        when(userRepository.findById(applicantId)).thenReturn(Optional.of(applicant));
        when(vendorApplicationRepository.existsByApplicant_UserIdAndApplicationStatus(applicantId, ApplicationStatus.PENDING))
                .thenReturn(false);
        when(vendorApplicationRepository.existsByTaxCode(request.getTaxCode())).thenReturn(false);
        when(vendorRepository.existsByTaxCode(request.getTaxCode())).thenReturn(false);
        when(vendorApplicationRepository.existsByContactEmail(request.getContactEmail())).thenReturn(false);
        when(vendorRepository.existsByContactEmail(request.getContactEmail())).thenReturn(false);
        when(vendorApplicationRepository.existsByContactPhone(request.getContactPhone())).thenReturn(false);
        when(vendorRepository.existsByContactPhone(request.getContactPhone())).thenReturn(true);

        // Act & Assert
        AppException exception = assertThrows(AppException.class, () ->
                vendorApplicationService.saveDraftApplication(applicantId, request)
        );
        assertEquals(ErrorCode.CONTACT_PHONE_ALREADY_EXISTS, exception.getErrorCode());
        assertEquals(ErrorCode.CONTACT_PHONE_ALREADY_EXISTS.getMessage(), exception.getMessage());
    }
}
