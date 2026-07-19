package com.sep.treksphere.service.impl;

import com.sep.treksphere.dto.request.BaseFilterRequest;
import com.sep.treksphere.dto.request.VendorStaffAddRequest;
import com.sep.treksphere.dto.response.PaginationResponse;
import com.sep.treksphere.dto.response.VendorStaffResponse;
import com.sep.treksphere.entity.Role;
import com.sep.treksphere.entity.User;
import com.sep.treksphere.entity.Vendor;
import com.sep.treksphere.entity.VendorStaff;
import com.sep.treksphere.exception.AppException;
import com.sep.treksphere.exception.ErrorCode;
import com.sep.treksphere.mapper.VendorStaffMapper;
import com.sep.treksphere.repository.RoleRepository;
import com.sep.treksphere.repository.UserRepository;
import com.sep.treksphere.repository.VendorRepository;
import com.sep.treksphere.repository.VendorStaffRepository;
import com.sep.treksphere.security.JwtTokenProvider;
import com.sep.treksphere.service.EmailService;
import com.sep.treksphere.service.VendorStaffService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class VendorStaffServiceImpl implements VendorStaffService {

    private final VendorRepository vendorRepository;
    private final VendorStaffRepository vendorStaffRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final VendorStaffMapper vendorStaffMapper;

    @Value("${application.frontend.url}")
    private String frontendUrl;

    @Override
    @Transactional(readOnly = true)
    public PaginationResponse<VendorStaffResponse> getMyVendorStaff(String managerEmail, BaseFilterRequest request) {
        Vendor vendor = vendorRepository.findByManager_Email(managerEmail)
                .orElseThrow(() -> new AppException(ErrorCode.VENDOR_NOT_FOUND));

        Page<VendorStaff> staffPage = vendorStaffRepository.findByVendorIdAndKeyword(
                vendor.getVendorId(),
                request.getKeyword(),
                request.getPageable()
        );

        List<VendorStaffResponse> responses = staffPage.getContent().stream()
                .map(vendorStaffMapper::toVendorStaffResponse)
                .toList();

        return PaginationResponse.<VendorStaffResponse>builder()
                .content(responses)
                .pageNumber(staffPage.getNumber())
                .pageSize(staffPage.getSize())
                .totalElements(staffPage.getTotalElements())
                .totalPages(staffPage.getTotalPages())
                .last(staffPage.isLast())
                .build();
    }

    @Override
    @Transactional
    public VendorStaffResponse addVendorStaff(String managerEmail, VendorStaffAddRequest request) {
        log.info("Manager {} is adding new vendor staff with email: {}", managerEmail, request.getEmail());
        
        Vendor vendor = vendorRepository.findByManager_Email(managerEmail)
                .orElseThrow(() -> new AppException(ErrorCode.VENDOR_NOT_FOUND));

        String email = request.getEmail().trim().toLowerCase();
        
        User user = userRepository.findByEmail(email).orElse(null);
        VendorStaff staff;
        
        Role staffRole = roleRepository.findByRoleName("VENDOR_STAFF")
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));

        if (user != null) {
            log.info("User with email {} already exists in system. Checking staff status...", email);
            staff = vendorStaffRepository.findByUser_UserId(user.getUserId()).orElse(null);
            if (staff != null && !staff.getIsDeleted()) {
                if (staff.getVendor().getVendorId().equals(vendor.getVendorId())) {
                    throw new AppException(ErrorCode.STAFF_ALREADY_EXISTS);
                } else {
                    throw new AppException(ErrorCode.STAFF_BELONGS_TO_OTHER_VENDOR);
                }
            }
            
            if (staff != null && staff.getIsDeleted()) {
                log.info("Restoring deleted staff record for user: {}", email);
                staff.setIsDeleted(false);
                staff.setIsActive(true);
                staff.setVendor(vendor);
            } else {
                staff = new VendorStaff();
                staff.setUser(user);
                staff.setVendor(vendor);
                staff.setIsActive(true);
            }
            
            if (!user.getRoles().contains(staffRole)) {
                user.getRoles().add(staffRole);
                userRepository.save(user);
            }
            
            staff = vendorStaffRepository.save(staff);
            
            try {
                emailService.sendStaffInvitationEmail(email, user.getFullName(), vendor.getCompanyName(), "Sử dụng mật khẩu hiện tại của bạn", frontendUrl);
            } catch (Exception e) {
                log.error("Failed to send welcome email to existing user: {}", email, e);
            }
            
        } else {
            log.info("User with email {} does not exist. Creating new user and sending invitation...", email);
            
            user = new User();
            user.setEmail(email);
            user.setFullName(StringUtils.hasText(request.getFullName()) ? request.getFullName().trim() : email.split("@")[0]);
            
            String defaultPassword = "TrekStaff@" + UUID.randomUUID().toString().substring(0, 8);
            user.setPasswordHash(passwordEncoder.encode(defaultPassword));
            user.setEmailVerified(false);
            
            Role trekkerRole = roleRepository.findByRoleName("TREKKER")
                    .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));
            
            user.getRoles().add(trekkerRole);
            user.getRoles().add(staffRole);
            user = userRepository.save(user);
            
            // Tạo bản ghi VendorStaff
            staff = new VendorStaff();
            staff.setUser(user);
            staff.setVendor(vendor);
            staff.setIsActive(true);
            staff = vendorStaffRepository.save(staff);
            
            // Gửi link kích hoạt
            String verificationToken = tokenProvider.generateVerificationToken(email);
            String activationUrl = frontendUrl + "/verify?token=" + verificationToken;
            
            try {
                emailService.sendStaffInvitationEmail(email, user.getFullName(), vendor.getCompanyName(), defaultPassword, activationUrl);
                log.info("Invitation email sent successfully to new staff: {}", email);
            } catch (Exception e) {
                log.error("Failed to send invitation email to new staff: {}", email, e);
            }
        }
        
        return vendorStaffMapper.toVendorStaffResponse(staff);
    }
}
