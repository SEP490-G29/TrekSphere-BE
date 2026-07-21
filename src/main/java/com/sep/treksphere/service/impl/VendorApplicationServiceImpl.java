package com.sep.treksphere.service.impl;

import com.sep.treksphere.dto.request.VendorApplicationFilterRequest;
import com.sep.treksphere.dto.request.VendorApplicationReviewRequest;
import com.sep.treksphere.dto.request.VendorApplicationRequest;
import com.sep.treksphere.dto.request.VendorApplicationUpdateRequest;
import com.sep.treksphere.dto.response.PaginationResponse;
import com.sep.treksphere.dto.response.VendorApplicationResponse;
import com.sep.treksphere.dto.response.VendorResponse;
import com.sep.treksphere.entity.Role;
import com.sep.treksphere.entity.User;
import com.sep.treksphere.entity.Vendor;
import com.sep.treksphere.entity.VendorApplication;
import com.sep.treksphere.enums.vendor.VendorStatus;
import com.sep.treksphere.exception.ErrorCode;
import com.sep.treksphere.enums.vendor.ApplicationStatus;
import com.sep.treksphere.exception.AppException;
import com.sep.treksphere.mapper.VendorApplicationMapper;
import com.sep.treksphere.mapper.VendorMapper;
import com.sep.treksphere.repository.RoleRepository;
import com.sep.treksphere.repository.UserRepository;
import com.sep.treksphere.repository.VendorApplicationRepository;
import com.sep.treksphere.repository.VendorRepository;
import com.sep.treksphere.security.CustomUserDetails;
import com.sep.treksphere.service.FileService;
import com.sep.treksphere.service.VendorApplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class VendorApplicationServiceImpl implements VendorApplicationService {

    private final VendorApplicationRepository vendorApplicationRepository;
    private final UserRepository userRepository;
    private final VendorRepository vendorRepository;
    private final VendorApplicationMapper vendorApplicationMapper;
    private final RoleRepository roleRepository;
    private final VendorMapper vendorMapper;
    private final FileService fileService;

    @Override
    @Transactional
    public VendorApplicationResponse saveDraftApplication(UUID applicantId, VendorApplicationRequest request) {
        log.info("Processing vendor application draft creation for user ID: {}", applicantId);

        User applicant = userRepository.findById(applicantId)
                .orElseThrow(() -> {
                    log.error("User with ID {} not found", applicantId);
                    return new AppException(ErrorCode.USER_NOT_FOUND);
                });

        boolean hasPending = vendorApplicationRepository.existsByApplicant_UserIdAndApplicationStatus(
                applicant.getUserId(), ApplicationStatus.PENDING
        );
        if (hasPending) {
            log.warn("User ID {} already has a PENDING vendor application", applicantId);
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
        vendorApplication.setApplicationStatus(ApplicationStatus.DRAFT);
        vendorApplication.setBusinessLicenseUrl(businessLicenseUrl);

        vendorApplication = vendorApplicationRepository.save(vendorApplication);
        log.info("Successfully created vendor application draft with ID: {} for user: {}", 
                vendorApplication.getVendorApplicationId(), applicantId);

        return vendorApplicationMapper.toResponse(vendorApplication);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginationResponse<VendorApplicationResponse> getApplications(VendorApplicationFilterRequest request) {
        log.info("Admin fetching vendor applications with filter - status: {}, keyword: {}", 
                request.getStatus(), request.getKeyword());

        Pageable pageable = request.getPageable();
        Page<VendorApplication> pageResult = vendorApplicationRepository.findAllApplicationsWithFilter(
                request.getStatus(),
                request.getKeyword(),
                pageable
        );

        List<VendorApplicationResponse> content = pageResult.getContent().stream()
                .map(vendorApplicationMapper::toResponse)
                .collect(Collectors.toList());

        return PaginationResponse.<VendorApplicationResponse>builder()
                .content(content)
                .pageNumber(pageResult.getNumber())
                .pageSize(pageResult.getSize())
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .last(pageResult.isLast())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public VendorApplicationResponse getApplicationById(UUID id, CustomUserDetails userDetails) {
        log.info("Fetching details of vendor application with ID: {} for user: {}", id, userDetails.getUsername());

        VendorApplication application = vendorApplicationRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Vendor application with ID {} not found", id);
                    return new AppException(ErrorCode.VENDOR_APPLICATION_NOT_FOUND);
                });

        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isOwner = application.getApplicant().getUserId().equals(userDetails.getUser().getUserId());

        if (!isAdmin && !isOwner) {
            log.warn("User {} attempted to view vendor application {} without permission", 
                    userDetails.getUser().getUserId(), id);
            throw new AppException(ErrorCode.UNAUTHORIZED_APPLICATION_ACCESS);
        }

        return vendorApplicationMapper.toResponse(application);
    }

    @Override
    @Transactional
    public VendorApplicationResponse reviewApplication(UUID id, VendorApplicationReviewRequest request) {
        log.info("Processing review for vendor application with ID: {} to status: {}", id, request.getStatus());

        VendorApplication application = vendorApplicationRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Vendor application with ID {} not found for review", id);
                    return new AppException(ErrorCode.VENDOR_APPLICATION_NOT_FOUND);
                });

        if (application.getApplicationStatus() != ApplicationStatus.PENDING) {
            log.warn("Vendor application {} is already processed. Current status: {}", 
                    id, application.getApplicationStatus());
            throw new AppException(ErrorCode.APPLICATION_ALREADY_PROCESSED);
        }

        if (request.getStatus() != ApplicationStatus.APPROVED && request.getStatus() != ApplicationStatus.REJECTED) {
            log.warn("Invalid review status: {}", request.getStatus());
            throw new AppException(ErrorCode.INVALID_REVIEW_STATUS);
        }

        if (request.getStatus() == ApplicationStatus.APPROVED) {
            application.setApplicationStatus(ApplicationStatus.APPROVED);
            application.setRejectionReason(null);
            vendorApplicationRepository.save(application);

            User applicant = application.getApplicant();
            Role managerRole = roleRepository.findByRoleName("VENDOR_MANAGER")
                    .orElseThrow(() -> {
                        log.error("Role VENDOR_MANAGER not found in database");
                        return new AppException(ErrorCode.ROLE_NOT_FOUND);
                    });

            boolean hasManagerRole = applicant.getRoles().stream()
                    .anyMatch(r -> r.getRoleName().equals("VENDOR_MANAGER"));
            if (!hasManagerRole) {
                applicant.getRoles().add(managerRole);
                userRepository.save(applicant);
                log.info("Role VENDOR_MANAGER successfully assigned to user: {}", applicant.getEmail());
            }

            Vendor vendor = new Vendor();
            vendor.setManager(applicant);
            vendor.setCompanyName(application.getCompanyName());
            vendor.setContactEmail(application.getContactEmail());
            vendor.setContactPhone(application.getContactPhone());
            vendor.setTaxCode(application.getTaxCode());
            vendor.setBusinessLicenseUrl(application.getBusinessLicenseUrl());
            vendor.setDescription(application.getBusinessDescription());
            vendor.setStatus(VendorStatus.ACTIVE);

            vendor = vendorRepository.save(vendor);
            log.info("Successfully created Vendor profile with ID: {} for company: {}", 
                    vendor.getVendorId(), vendor.getCompanyName());
        } else {
            if (!StringUtils.hasText(request.getRejectionReason())) {
                log.warn("Rejection reason is required when status is REJECTED");
                throw new AppException(ErrorCode.REJECTION_REASON_REQUIRED);
            }

            application.setApplicationStatus(ApplicationStatus.REJECTED);
            application.setRejectionReason(request.getRejectionReason().trim());
            vendorApplicationRepository.save(application);
            log.info("Successfully rejected vendor application with ID: {}", id);
        }

        return vendorApplicationMapper.toResponse(application);
    }

    @Override
    @Transactional
    public VendorApplicationResponse updateApplication(UUID id, VendorApplicationUpdateRequest request, UUID applicantId) {
        log.info("Updating vendor application with ID: {} for user ID: {}", id, applicantId);

        VendorApplication application = vendorApplicationRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Vendor application with ID {} not found for update", id);
                    return new AppException(ErrorCode.VENDOR_APPLICATION_NOT_FOUND);
                });

        if (!application.getApplicant().getUserId().equals(applicantId)) {
            log.warn("User ID {} attempted to update vendor application {} owned by {}", 
                    applicantId, id, application.getApplicant().getUserId());
            throw new AppException(ErrorCode.UNAUTHORIZED_APPLICATION_ACCESS);
        }

        if (application.getApplicationStatus() != ApplicationStatus.DRAFT &&
            application.getApplicationStatus() != ApplicationStatus.REJECTED) {
            log.warn("Cannot update vendor application {}. Current status is: {}", 
                    id, application.getApplicationStatus());
            throw new AppException(ErrorCode.CANNOT_UPDATE_APPLICATION);
        }

        if (StringUtils.hasText(request.getTaxCode())) {
            String newTaxCode = request.getTaxCode().trim();
            if (!newTaxCode.equals(application.getTaxCode())) {
                boolean existsInApps = vendorApplicationRepository
                        .existsByTaxCodeAndVendorApplicationIdNot(newTaxCode, id);
                boolean existsInVendors = vendorRepository.existsByTaxCode(newTaxCode);
                if (existsInApps || existsInVendors) {
                    log.warn("Tax code {} is already registered during update of application {}", newTaxCode, id);
                    throw new AppException(ErrorCode.TAX_CODE_ALREADY_EXISTS);
                }
                application.setTaxCode(newTaxCode);
            }
        }

        if (StringUtils.hasText(request.getContactEmail())) {
            String newEmail = request.getContactEmail().trim();
            if (!newEmail.equalsIgnoreCase(application.getContactEmail())) {
                boolean existsInApps = vendorApplicationRepository
                        .existsByContactEmailAndVendorApplicationIdNot(newEmail, id);
                boolean existsInVendors = vendorRepository.existsByContactEmail(newEmail);
                if (existsInApps || existsInVendors) {
                    log.warn("Contact email {} is already registered during update of application {}", newEmail, id);
                    throw new AppException(ErrorCode.CONTACT_EMAIL_ALREADY_EXISTS);
                }
                application.setContactEmail(newEmail);
            }
        }

        if (StringUtils.hasText(request.getContactPhone())) {
            String newPhone = request.getContactPhone().trim();
            if (!newPhone.equals(application.getContactPhone())) {
                boolean existsInApps = vendorApplicationRepository
                        .existsByContactPhoneAndVendorApplicationIdNot(newPhone, id);
                boolean existsInVendors = vendorRepository.existsByContactPhone(newPhone);
                if (existsInApps || existsInVendors) {
                    log.warn("Contact phone {} is already registered during update of application {}", newPhone, id);
                    throw new AppException(ErrorCode.CONTACT_PHONE_ALREADY_EXISTS);
                }
                application.setContactPhone(newPhone);
            }
        }

        if (StringUtils.hasText(request.getCompanyName())) {
            application.setCompanyName(request.getCompanyName().trim());
        }
        if (StringUtils.hasText(request.getBusinessDescription())) {
            application.setBusinessDescription(request.getBusinessDescription().trim());
        }

        if (request.getBusinessLicense() != null && !request.getBusinessLicense().isEmpty()) {
            log.info("Uploading new business license file for vendor application update");
            String newUrl = fileService.uploadFile(request.getBusinessLicense(), "vendor-licenses");
            application.setBusinessLicenseUrl(newUrl);
        }

        application = vendorApplicationRepository.save(application);
        log.info("Successfully updated vendor application with ID: {}", id);

        return vendorApplicationMapper.toResponse(application);
    }

    @Override
    @Transactional
    public VendorApplicationResponse submitDraftApplication(UUID id, UUID applicantId) {
        log.info("Submitting draft vendor application with ID: {} for user ID: {}", id, applicantId);

        VendorApplication application = vendorApplicationRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Vendor application with ID {} not found for submission", id);
                    return new AppException(ErrorCode.VENDOR_APPLICATION_NOT_FOUND);
                });

        if (!application.getApplicant().getUserId().equals(applicantId)) {
            log.warn("User ID {} attempted to submit vendor application {} owned by {}", 
                    applicantId, id, application.getApplicant().getUserId());
            throw new AppException(ErrorCode.UNAUTHORIZED_APPLICATION_ACCESS);
        }

        if (application.getApplicationStatus() != ApplicationStatus.DRAFT) {
            log.warn("Cannot submit vendor application {}. Current status is: {}", 
                    id, application.getApplicationStatus());
            throw new AppException(ErrorCode.CANNOT_SUBMIT_APPLICATION);
        }

        boolean isTaxCodeExistInApplications = vendorApplicationRepository
                .existsByTaxCodeAndVendorApplicationIdNot(application.getTaxCode(), id);
        boolean isTaxCodeExistInVendors = vendorRepository.existsByTaxCode(application.getTaxCode());
        if (isTaxCodeExistInApplications || isTaxCodeExistInVendors) {
            log.warn("Tax code {} already exists during submission of application {}", application.getTaxCode(), id);
            throw new AppException(ErrorCode.TAX_CODE_ALREADY_EXISTS);
        }

        boolean isEmailExistInApplications = vendorApplicationRepository
                .existsByContactEmailAndVendorApplicationIdNot(application.getContactEmail(), id);
        boolean isEmailExistInVendors = vendorRepository.existsByContactEmail(application.getContactEmail());
        if (isEmailExistInApplications || isEmailExistInVendors) {
            log.warn("Contact email {} already exists during submission of application {}", application.getContactEmail(), id);
            throw new AppException(ErrorCode.CONTACT_EMAIL_ALREADY_EXISTS);
        }

        boolean isPhoneExistInApplications = vendorApplicationRepository
                .existsByContactPhoneAndVendorApplicationIdNot(application.getContactPhone(), id);
        boolean isPhoneExistInVendors = vendorRepository.existsByContactPhone(application.getContactPhone());
        if (isPhoneExistInApplications || isPhoneExistInVendors) {
            log.warn("Contact phone {} already exists during submission of application {}", application.getContactPhone(), id);
            throw new AppException(ErrorCode.CONTACT_PHONE_ALREADY_EXISTS);
        }

        application.setApplicationStatus(ApplicationStatus.PENDING);

        application = vendorApplicationRepository.save(application);
        log.info("Successfully submitted draft vendor application with ID: {}", id);

        return vendorApplicationMapper.toResponse(application);
    }

    @Override
    @Transactional
    public VendorApplicationResponse resubmitRejectedApplication(UUID id, UUID applicantId) {
        log.info("Resubmitting rejected vendor application with ID: {} for user ID: {}", id, applicantId);

        VendorApplication application = vendorApplicationRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Vendor application with ID {} not found for resubmission", id);
                    return new AppException(ErrorCode.VENDOR_APPLICATION_NOT_FOUND);
                });

        if (!application.getApplicant().getUserId().equals(applicantId)) {
            log.warn("User ID {} attempted to resubmit vendor application {} owned by {}", 
                    applicantId, id, application.getApplicant().getUserId());
            throw new AppException(ErrorCode.UNAUTHORIZED_APPLICATION_ACCESS);
        }

        if (application.getApplicationStatus() != ApplicationStatus.REJECTED) {
            log.warn("Cannot resubmit vendor application {}. Current status is: {}", 
                    id, application.getApplicationStatus());
            throw new AppException(ErrorCode.CANNOT_RESUBMIT_APPLICATION);
        }

        boolean isTaxCodeExistInApplications = vendorApplicationRepository
                .existsByTaxCodeAndVendorApplicationIdNot(application.getTaxCode(), id);
        boolean isTaxCodeExistInVendors = vendorRepository.existsByTaxCode(application.getTaxCode());
        if (isTaxCodeExistInApplications || isTaxCodeExistInVendors) {
            log.warn("Tax code {} already exists during resubmission of application {}", application.getTaxCode(), id);
            throw new AppException(ErrorCode.TAX_CODE_ALREADY_EXISTS);
        }

        boolean isEmailExistInApplications = vendorApplicationRepository
                .existsByContactEmailAndVendorApplicationIdNot(application.getContactEmail(), id);
        boolean isEmailExistInVendors = vendorRepository.existsByContactEmail(application.getContactEmail());
        if (isEmailExistInApplications || isEmailExistInVendors) {
            log.warn("Contact email {} already exists during resubmission of application {}", application.getContactEmail(), id);
            throw new AppException(ErrorCode.CONTACT_EMAIL_ALREADY_EXISTS);
        }

        boolean isPhoneExistInApplications = vendorApplicationRepository
                .existsByContactPhoneAndVendorApplicationIdNot(application.getContactPhone(), id);
        boolean isPhoneExistInVendors = vendorRepository.existsByContactPhone(application.getContactPhone());
        if (isPhoneExistInApplications || isPhoneExistInVendors) {
            log.warn("Contact phone {} already exists during resubmission of application {}", application.getContactPhone(), id);
            throw new AppException(ErrorCode.CONTACT_PHONE_ALREADY_EXISTS);
        }

        application.setApplicationStatus(ApplicationStatus.PENDING);
        application.setRejectionReason(null);

        application = vendorApplicationRepository.save(application);
        log.info("Successfully resubmitted vendor application with ID: {}", id);

        return vendorApplicationMapper.toResponse(application);
    }
}
