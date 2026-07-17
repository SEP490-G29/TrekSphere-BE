package com.sep.treksphere.service.impl;

import com.sep.treksphere.dto.request.VendorApplicationFilterRequest;
import com.sep.treksphere.dto.request.VendorApplicationRequest;
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
    public VendorResponse approveApplication(UUID id) {
        log.info("Processing approval for vendor application with ID: {}", id);

        VendorApplication application = vendorApplicationRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Vendor application with ID {} not found for approval", id);
                    return new AppException(ErrorCode.VENDOR_APPLICATION_NOT_FOUND);
                });

        if (application.getApplicationStatus() != ApplicationStatus.PENDING) {
            log.warn("Vendor application {} is already processed. Current status: {}", 
                    id, application.getApplicationStatus());
            throw new AppException(ErrorCode.APPLICATION_ALREADY_PROCESSED);
        }

        application.setApplicationStatus(ApplicationStatus.APPROVED);
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

        return vendorMapper.toVendorResponse(vendor);
    }
}
