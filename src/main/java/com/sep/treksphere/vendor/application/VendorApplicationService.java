package com.sep.treksphere.vendor.application;

import com.sep.treksphere.common.dto.PaginationResponse;
import com.sep.treksphere.common.exception.AppException;
import com.sep.treksphere.common.exception.ErrorCode;
import com.sep.treksphere.common.security.CustomUserDetails;
import com.sep.treksphere.file.FileService;
import com.sep.treksphere.file.UploadPolicy;
import com.sep.treksphere.notification.NotificationEventType;
import com.sep.treksphere.notification.NotificationService;
import com.sep.treksphere.notification.ReferenceType;
import com.sep.treksphere.user.Role;
import com.sep.treksphere.user.RoleRepository;
import com.sep.treksphere.user.User;
import com.sep.treksphere.user.UserRepository;
import com.sep.treksphere.vendor.Vendor;
import com.sep.treksphere.vendor.VendorRepository;
import com.sep.treksphere.vendor.VendorStatus;
import com.sep.treksphere.vendor.application.dto.request.AdminVendorApplicationFilterRequest;
import com.sep.treksphere.vendor.application.dto.request.VendorApplicationFilterRequest;
import com.sep.treksphere.vendor.application.dto.request.VendorApplicationReviewRequest;
import com.sep.treksphere.vendor.application.dto.request.VendorApplicationRequest;
import com.sep.treksphere.vendor.application.dto.request.VendorApplicationUpdateRequest;
import com.sep.treksphere.vendor.application.dto.response.VendorApplicationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class VendorApplicationService {

    private static final Set<ApplicationStatus> ADMIN_VISIBLE_STATUSES = Set.of(
            ApplicationStatus.PENDING,
            ApplicationStatus.APPROVED,
            ApplicationStatus.REJECTED
    );

    private final VendorApplicationRepository vendorApplicationRepository;
    private final UserRepository userRepository;
    private final VendorRepository vendorRepository;
    private final VendorApplicationMapper vendorApplicationMapper;
    private final RoleRepository roleRepository;
    private final FileService fileService;
    private final NotificationService notificationService;

    private static final String ADMIN_APPLICATION_ACTION_URL_PREFIX = "/admin/applications/";
    private static final String APPLICANT_APPLICATIONS_URL = "/trekker/vendor-applications";

    private void notifyAdminsOfNewApplication(VendorApplication application) {
        List<UUID> adminIds = userRepository.findDistinctByRoles_RoleNameAndIsDeletedFalse("ADMIN").stream()
                .map(User::getUserId)
                .toList();

        notificationService.notify(
                adminIds,
                NotificationEventType.VENDOR_APPLICATION_SUBMITTED,
                ReferenceType.VENDOR_APPLICATION, application.getVendorApplicationId(),
                ADMIN_APPLICATION_ACTION_URL_PREFIX + application.getVendorApplicationId(),
                application.getApplicant().getFullName());
    }

    @Transactional
    public VendorApplicationResponse saveDraftApplication(UUID applicantId, VendorApplicationRequest request) {
        log.info("Processing vendor application draft creation for user ID: {}", applicantId);

        User applicant = getApplicantForUpdate(applicantId);
        ensureApplicantHasNoVendor(applicantId);
        ensureApplicantHasNoActiveApplication(applicantId);

        validateUniqueApplicationFields(request.getTaxCode(), request.getContactEmail(), request.getContactPhone(), null);

        String businessLicenseUrl = request.getBusinessLicense() == null || request.getBusinessLicense().isEmpty()
                ? null
                : fileService.upload(request.getBusinessLicense(), "vendor-licenses", UploadPolicy.BUSINESS_LICENSE).url();

        VendorApplication vendorApplication = vendorApplicationMapper.toEntity(request);
        vendorApplication.setApplicant(applicant);
        vendorApplication.setApplicationStatus(ApplicationStatus.DRAFT);
        vendorApplication.setBusinessLicenseUrl(businessLicenseUrl);
        normalizeApplication(vendorApplication);

        vendorApplication = vendorApplicationRepository.save(vendorApplication);
        log.info("Successfully created vendor application draft with ID: {} for user: {}",
                vendorApplication.getVendorApplicationId(), applicantId);

        return vendorApplicationMapper.toResponse(vendorApplication);
    }

    @Transactional(readOnly = true)
    public PaginationResponse<VendorApplicationResponse> getApplications(AdminVendorApplicationFilterRequest request) {
        log.info("Admin fetching vendor applications with filter - status: {}, keyword: {}",
                request.getStatus(), request.getKeyword());

        if (request.getStatus() != null && !ADMIN_VISIBLE_STATUSES.contains(request.getStatus())) {
            log.warn("Admin attempted to filter vendor applications by unsupported status: {}", request.getStatus());
            throw new AppException(ErrorCode.INVALID_APPLICATION_FILTER_STATUS);
        }

        Pageable pageable = request.getPageable();
        Page<VendorApplication> pageResult = vendorApplicationRepository.findAllApplicationsWithFilter(
                ADMIN_VISIBLE_STATUSES,
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

    @Transactional(readOnly = true)
    public PaginationResponse<VendorApplicationResponse> getMyApplicationHistory(
            UUID applicantId,
            VendorApplicationFilterRequest request
    ) {
        log.info("Fetching vendor application history for applicant: {}, status: {}, keyword: {}",
                applicantId, request.getStatus(), request.getKeyword());

        Pageable pageable = request.getPageable();
        Page<VendorApplication> pageResult = vendorApplicationRepository.findMyApplicationsWithFilter(
                applicantId,
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

    @Transactional(readOnly = true)
    public VendorApplicationResponse getApplicationById(UUID id, CustomUserDetails userDetails) {
        log.info("Fetching details of vendor application with ID: {} for user: {}", id, userDetails.getUsername());

        VendorApplication application = vendorApplicationRepository.findByVendorApplicationIdAndIsDeletedFalse(id)
                .orElseThrow(() -> {
                    log.error("Vendor application with ID {} not found", id);
                    return new AppException(ErrorCode.VENDOR_APPLICATION_NOT_FOUND);
                });

        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isOwner = application.getApplicant().getUserId().equals(userDetails.getUser().getUserId());

        if (isAdmin && !isOwner && application.getApplicationStatus() == ApplicationStatus.DRAFT) {
            throw new AppException(ErrorCode.VENDOR_APPLICATION_NOT_FOUND);
        }
        if (!isAdmin && !isOwner) {
            log.warn("User {} attempted to view vendor application {} without permission",
                    userDetails.getUser().getUserId(), id);
            throw new AppException(ErrorCode.UNAUTHORIZED_APPLICATION_ACCESS);
        }

        return vendorApplicationMapper.toResponse(application);
    }

    @Transactional
    public VendorApplicationResponse reviewApplication(UUID id, VendorApplicationReviewRequest request, UUID reviewerId) {
        log.info("Processing review for vendor application with ID: {} to status: {}", id, request.getStatus());

        VendorApplication application = getApplicationWithApplicantLock(id);
        User reviewer = userRepository.findById(reviewerId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

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
            ensureApplicantHasNoVendor(application.getApplicant().getUserId());
            ensureApplicantHasNoOtherActiveApplication(
                    application.getApplicant().getUserId(),
                    application.getVendorApplicationId()
            );

            validateSubmissionCompleteness(application);

            User applicant = application.getApplicant();
            Role vendorRole = roleRepository.findByRoleName("VENDOR")
                    .orElseThrow(() -> {
                        log.error("Role VENDOR not found in database");
                        return new AppException(ErrorCode.ROLE_NOT_FOUND);
                    });

            boolean hasVendorRole = applicant.getRoles().stream()
                    .anyMatch(r -> r.getRoleName().equals("VENDOR"));
            if (!hasVendorRole) {
                applicant.getRoles().add(vendorRole);
                userRepository.save(applicant);
                log.info("Role VENDOR successfully assigned to user: {}", applicant.getEmail());
            }

            Vendor vendor = new Vendor();
            vendor.setManager(applicant);
            vendor.setCompanyName(application.getCompanyName());
            vendor.setContactEmail(application.getContactEmail());
            vendor.setContactPhone(application.getContactPhone());
            vendor.setTaxCode(application.getTaxCode());
            vendor.setBusinessLicenseUrl(application.getBusinessLicenseUrl());
            vendor.setDescription(application.getBusinessDescription());
            vendor.setBusinessAddress(application.getBusinessAddress());
            vendor.setLegalRepresentativeName(application.getLegalRepresentativeName());
            vendor.setLegalRepresentativePosition(application.getLegalRepresentativePosition());
            vendor.setWebsiteUrl(application.getWebsiteUrl());
            vendor.setStatus(VendorStatus.ACTIVE);

            vendor = vendorRepository.save(vendor);
            application.setApplicationStatus(ApplicationStatus.APPROVED);
            application.setRejectionReason(null);
            application.setVendor(vendor);
            application.setReviewedBy(reviewer);
            application.setReviewedAt(LocalDateTime.now());
            vendorApplicationRepository.save(application);
            log.info("Successfully created Vendor profile with ID: {} for company: {}",
                    vendor.getVendorId(), vendor.getCompanyName());

            notificationService.notify(
                    applicant.getUserId(),
                    NotificationEventType.VENDOR_APPLICATION_APPROVED,
                    ReferenceType.VENDOR_APPLICATION, application.getVendorApplicationId(),
                    APPLICANT_APPLICATIONS_URL);
        } else {
            if (!StringUtils.hasText(request.getRejectionReason())) {
                log.warn("Rejection reason is required when status is REJECTED");
                throw new AppException(ErrorCode.REJECTION_REASON_REQUIRED);
            }

            application.setApplicationStatus(ApplicationStatus.REJECTED);
            application.setRejectionReason(request.getRejectionReason().trim());
            application.setReviewedBy(reviewer);
            application.setReviewedAt(LocalDateTime.now());
            vendorApplicationRepository.save(application);
            log.info("Successfully rejected vendor application with ID: {}", id);

            notificationService.notify(
                    application.getApplicant().getUserId(),
                    NotificationEventType.VENDOR_APPLICATION_REJECTED,
                    ReferenceType.VENDOR_APPLICATION, application.getVendorApplicationId(),
                    APPLICANT_APPLICATIONS_URL,
                    application.getRejectionReason());
        }

        return vendorApplicationMapper.toResponse(application);
    }

    @Transactional
    public VendorApplicationResponse updateApplication(UUID id, VendorApplicationUpdateRequest request, UUID applicantId) {
        log.info("Updating vendor application with ID: {} for user ID: {}", id, applicantId);

        VendorApplication application = getApplicationWithApplicantLock(id);

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
        if (request.getBusinessAddress() != null) {
            application.setBusinessAddress(trimToNull(request.getBusinessAddress()));
        }
        if (request.getLegalRepresentativeName() != null) {
            application.setLegalRepresentativeName(trimToNull(request.getLegalRepresentativeName()));
        }
        if (request.getLegalRepresentativePosition() != null) {
            application.setLegalRepresentativePosition(trimToNull(request.getLegalRepresentativePosition()));
        }
        if (request.getWebsiteUrl() != null) {
            application.setWebsiteUrl(trimToNull(request.getWebsiteUrl()));
        }

        if (request.getBusinessLicense() != null && !request.getBusinessLicense().isEmpty()) {
            log.info("Uploading new business license file for vendor application update");
            String newUrl = fileService.upload(
                    request.getBusinessLicense(), "vendor-licenses", UploadPolicy.BUSINESS_LICENSE).url();
            application.setBusinessLicenseUrl(newUrl);
        }

        application = vendorApplicationRepository.save(application);
        log.info("Successfully updated vendor application with ID: {}", id);

        return vendorApplicationMapper.toResponse(application);
    }

    @Transactional
    public VendorApplicationResponse submitDraftApplication(UUID id, UUID applicantId) {
        log.info("Submitting draft vendor application with ID: {} for user ID: {}", id, applicantId);

        VendorApplication application = getApplicationWithApplicantLock(id);

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

        ensureApplicantHasNoVendor(applicantId);
        ensureApplicantHasNoOtherActiveApplication(applicantId, id);
        validateSubmissionCompleteness(application);
        validateUniqueApplicationFields(
                application.getTaxCode(), application.getContactEmail(), application.getContactPhone(), id);

        application.setApplicationStatus(ApplicationStatus.PENDING);

        application = vendorApplicationRepository.save(application);
        log.info("Successfully submitted draft vendor application with ID: {}", id);

        notifyAdminsOfNewApplication(application);

        return vendorApplicationMapper.toResponse(application);
    }

    @Transactional
    public VendorApplicationResponse resubmitRejectedApplication(UUID id, UUID applicantId) {
        log.info("Resubmitting rejected vendor application with ID: {} for user ID: {}", id, applicantId);

        VendorApplication application = getApplicationWithApplicantLock(id);

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

        ensureApplicantHasNoVendor(applicantId);
        ensureApplicantHasNoOtherActiveApplication(applicantId, id);
        validateSubmissionCompleteness(application);
        validateUniqueApplicationFields(
                application.getTaxCode(), application.getContactEmail(), application.getContactPhone(), id);

        application.setApplicationStatus(ApplicationStatus.PENDING);
        application.setRejectionReason(null);
        application.setReviewedBy(null);
        application.setReviewedAt(null);

        application = vendorApplicationRepository.save(application);
        log.info("Successfully resubmitted vendor application with ID: {}", id);

        notifyAdminsOfNewApplication(application);

        return vendorApplicationMapper.toResponse(application);
    }

    private User getApplicantForUpdate(UUID applicantId) {
        return userRepository.findByIdForUpdate(applicantId)
                .orElseThrow(() -> {
                    log.error("User with ID {} not found", applicantId);
                    return new AppException(ErrorCode.USER_NOT_FOUND);
                });
    }

    private VendorApplication getApplicationWithApplicantLock(UUID applicationId) {
        UUID applicantId = vendorApplicationRepository.findApplicantIdByApplicationId(applicationId)
                .orElseThrow(() -> {
                    log.error("Active vendor application with ID {} not found", applicationId);
                    return new AppException(ErrorCode.VENDOR_APPLICATION_NOT_FOUND);
                });

        getApplicantForUpdate(applicantId);
        return vendorApplicationRepository.findByVendorApplicationIdAndIsDeletedFalse(applicationId)
                .orElseThrow(() -> new AppException(ErrorCode.VENDOR_APPLICATION_NOT_FOUND));
    }

    private void ensureApplicantHasNoActiveApplication(UUID applicantId) {
        if (vendorApplicationRepository.existsByApplicant_UserIdAndIsDeletedFalse(applicantId)) {
            log.warn("Applicant {} already has an active vendor application", applicantId);
            throw new AppException(ErrorCode.APPLICANT_ALREADY_HAS_APPLICATION);
        }
    }

    private void ensureApplicantHasNoOtherActiveApplication(UUID applicantId, UUID applicationId) {
        if (vendorApplicationRepository
                .existsByApplicant_UserIdAndVendorApplicationIdNotAndIsDeletedFalse(applicantId, applicationId)) {
            log.warn("Applicant {} has another active vendor application besides {}", applicantId, applicationId);
            throw new AppException(ErrorCode.APPLICANT_ALREADY_HAS_APPLICATION);
        }
    }

    private void ensureApplicantHasNoVendor(UUID applicantId) {
        if (vendorRepository.existsByManager_UserIdAndIsDeletedFalse(applicantId)) {
            log.warn("Applicant {} already manages a vendor", applicantId);
            throw new AppException(ErrorCode.APPLICANT_ALREADY_HAS_VENDOR);
        }
    }

    private void validateSubmissionCompleteness(VendorApplication application) {
        if (!StringUtils.hasText(application.getCompanyName())
                || !StringUtils.hasText(application.getContactEmail())
                || !StringUtils.hasText(application.getContactPhone())
                || !StringUtils.hasText(application.getTaxCode())
                || !StringUtils.hasText(application.getBusinessLicenseUrl())
                || !StringUtils.hasText(application.getBusinessDescription())
                || !StringUtils.hasText(application.getBusinessAddress())
                || !StringUtils.hasText(application.getLegalRepresentativeName())
                || !StringUtils.hasText(application.getLegalRepresentativePosition())) {
            throw new AppException(
                    ErrorCode.VENDOR_APPLICATION_INCOMPLETE,
                    "Hồ sơ phải có đầy đủ thông tin doanh nghiệp, người đại diện và giấy phép trước khi nộp.");
        }
    }

    private void validateUniqueApplicationFields(String taxCode, String email, String phone, UUID currentId) {
        if (StringUtils.hasText(taxCode)) {
            String value = taxCode.trim();
            boolean existsInApps = currentId == null
                    ? vendorApplicationRepository.existsByTaxCode(value)
                    : vendorApplicationRepository.existsByTaxCodeAndVendorApplicationIdNot(value, currentId);
            if (existsInApps || vendorRepository.existsByTaxCode(value)) {
                throw new AppException(ErrorCode.TAX_CODE_ALREADY_EXISTS);
            }
        }
        if (StringUtils.hasText(email)) {
            String value = email.trim();
            boolean existsInApps = currentId == null
                    ? vendorApplicationRepository.existsByContactEmail(value)
                    : vendorApplicationRepository.existsByContactEmailAndVendorApplicationIdNot(value, currentId);
            if (existsInApps || vendorRepository.existsByContactEmail(value)) {
                throw new AppException(ErrorCode.CONTACT_EMAIL_ALREADY_EXISTS);
            }
        }
        if (StringUtils.hasText(phone)) {
            String value = phone.trim();
            boolean existsInApps = currentId == null
                    ? vendorApplicationRepository.existsByContactPhone(value)
                    : vendorApplicationRepository.existsByContactPhoneAndVendorApplicationIdNot(value, currentId);
            if (existsInApps || vendorRepository.existsByContactPhone(value)) {
                throw new AppException(ErrorCode.CONTACT_PHONE_ALREADY_EXISTS);
            }
        }
    }

    private void normalizeApplication(VendorApplication application) {
        application.setCompanyName(trimToNull(application.getCompanyName()));
        application.setContactEmail(trimToNull(application.getContactEmail()));
        application.setContactPhone(trimToNull(application.getContactPhone()));
        application.setTaxCode(trimToNull(application.getTaxCode()));
        application.setBusinessDescription(trimToNull(application.getBusinessDescription()));
        application.setBusinessAddress(trimToNull(application.getBusinessAddress()));
        application.setLegalRepresentativeName(trimToNull(application.getLegalRepresentativeName()));
        application.setLegalRepresentativePosition(trimToNull(application.getLegalRepresentativePosition()));
        application.setWebsiteUrl(trimToNull(application.getWebsiteUrl()));
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
