package com.sep.treksphere.service.impl;

import com.sep.treksphere.dto.request.VendorApplicationRequest;
import com.sep.treksphere.dto.response.VendorApplicationResponse;
import com.sep.treksphere.entity.User;
import com.sep.treksphere.entity.VendorApplication;
import com.sep.treksphere.exception.ErrorCode;
import com.sep.treksphere.enums.vendor.ApplicationStatus;
import com.sep.treksphere.exception.AppException;
import com.sep.treksphere.mapper.VendorApplicationMapper;
import com.sep.treksphere.repository.UserRepository;
import com.sep.treksphere.repository.VendorApplicationRepository;
import com.sep.treksphere.repository.VendorRepository;
import com.sep.treksphere.service.FileService;
import com.sep.treksphere.service.VendorApplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class VendorApplicationServiceImpl implements VendorApplicationService {

    private final VendorApplicationRepository vendorApplicationRepository;
    private final UserRepository userRepository;
    private final VendorRepository vendorRepository;
    private final VendorApplicationMapper vendorApplicationMapper;
    private final FileService fileService;

    @Override
    @Transactional
    public VendorApplicationResponse submitApplication(String applicantEmail, VendorApplicationRequest request) {
        log.info("Processing vendor application submission for user email: {}", applicantEmail);

        User applicant = userRepository.findByEmail(applicantEmail)
                .orElseThrow(() -> {
                    log.error("User with email {} not found", applicantEmail);
                    return new AppException(ErrorCode.USER_NOT_FOUND);
                });

        boolean hasPending = vendorApplicationRepository.existsByApplicant_UserIdAndApplicationStatus(
                applicant.getUserId(), ApplicationStatus.PENDING
        );
        if (hasPending) {
            log.warn("User {} already has a PENDING vendor application", applicantEmail);
            throw new AppException(ErrorCode.APPLICATION_PENDING_EXISTS);
        }

        boolean isTaxCodeExistInApplications = vendorApplicationRepository.existsByTaxCode(request.getTaxCode());
        boolean isTaxCodeExistInVendors = vendorRepository.existsByTaxCode(request.getTaxCode());
        if (isTaxCodeExistInApplications || isTaxCodeExistInVendors) {
            log.warn("Tax code {} already exists in the system", request.getTaxCode());
            throw new AppException(ErrorCode.TAX_CODE_ALREADY_EXISTS);
        }

        boolean isEmailExistInApplications = vendorApplicationRepository.existsByContactEmail(request.getContactEmail());
        boolean isEmailExistInVendors = vendorRepository.existsByContactEmail(request.getContactEmail());
        if (isEmailExistInApplications || isEmailExistInVendors) {
            log.warn("Contact email {} already exists in the system", request.getContactEmail());
            throw new AppException(ErrorCode.CONTACT_EMAIL_ALREADY_EXISTS);
        }

        boolean isPhoneExistInApplications = vendorApplicationRepository.existsByContactPhone(request.getContactPhone());
        boolean isPhoneExistInVendors = vendorRepository.existsByContactPhone(request.getContactPhone());
        if (isPhoneExistInApplications || isPhoneExistInVendors) {
            log.warn("Contact phone {} already exists in the system", request.getContactPhone());
            throw new AppException(ErrorCode.CONTACT_PHONE_ALREADY_EXISTS);
        }

        String businessLicenseUrl = fileService.uploadFile(request.getBusinessLicense(), "vendor-licenses");

        VendorApplication vendorApplication = vendorApplicationMapper.toEntity(request);
        vendorApplication.setApplicant(applicant);
        vendorApplication.setApplicationStatus(ApplicationStatus.PENDING);
        vendorApplication.setBusinessLicenseUrl(businessLicenseUrl);

        vendorApplication = vendorApplicationRepository.save(vendorApplication);
        log.info("Successfully created vendor application with ID: {} for user: {}", 
                vendorApplication.getVendorApplicationId(), applicantEmail);

        return vendorApplicationMapper.toResponse(vendorApplication);
    }
}
