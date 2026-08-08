package com.sep.treksphere.service.impl;

import com.sep.treksphere.dto.request.VendorStaffRoleUpdateRequest;
import com.sep.treksphere.dto.response.VendorStaffResponse;
import com.sep.treksphere.entity.Role;
import com.sep.treksphere.entity.User;
import com.sep.treksphere.entity.Vendor;
import com.sep.treksphere.entity.VendorStaff;
import com.sep.treksphere.enums.user.VendorStaffRole;
import com.sep.treksphere.exception.AppException;
import com.sep.treksphere.exception.ErrorCode;
import com.sep.treksphere.mapper.VendorStaffMapper;
import com.sep.treksphere.repository.CoordinatorScheduleRepository;
import com.sep.treksphere.repository.RoleRepository;
import com.sep.treksphere.repository.UserRepository;
import com.sep.treksphere.repository.VendorRepository;
import com.sep.treksphere.repository.VendorStaffRepository;
import com.sep.treksphere.security.JwtTokenProvider;
import com.sep.treksphere.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VendorStaffServiceImplTest {

    @Mock private VendorRepository vendorRepository;
    @Mock private VendorStaffRepository vendorStaffRepository;
    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private EmailService emailService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenProvider tokenProvider;
    @Mock private VendorStaffMapper vendorStaffMapper;
    @Mock private CoordinatorScheduleRepository coordinatorScheduleRepository;

    @InjectMocks private VendorStaffServiceImpl service;

    private final String managerEmail = "manager@vendor.test";
    private UUID vendorId;
    private UUID staffId;
    private Vendor vendor;
    private VendorStaff staff;
    private User user;

    @BeforeEach
    void setUp() {
        vendorId = UUID.randomUUID();
        staffId = UUID.randomUUID();

        vendor = new Vendor();
        vendor.setVendorId(vendorId);

        user = new User();
        user.setUserId(UUID.randomUUID());
        user.getRoles().add(role(VendorStaffRole.VENDOR_STAFF));

        staff = new VendorStaff();
        staff.setVendorStaffId(staffId);
        staff.setVendor(vendor);
        staff.setUser(user);
        staff.setIsDeleted(false);
    }

    @Test
    void updateVendorStaffRole_promotesStaffToCoordinator() {
        VendorStaffRoleUpdateRequest request = request(VendorStaffRole.COORDINATOR);
        Role coordinatorRole = role(VendorStaffRole.COORDINATOR);
        VendorStaffResponse expected = new VendorStaffResponse();

        when(vendorRepository.findByManager_Email(managerEmail)).thenReturn(Optional.of(vendor));
        when(vendorStaffRepository.findById(staffId)).thenReturn(Optional.of(staff));
        when(roleRepository.findByRoleName("COORDINATOR")).thenReturn(Optional.of(coordinatorRole));
        when(vendorStaffMapper.toVendorStaffResponse(staff)).thenReturn(expected);

        VendorStaffResponse actual = service.updateVendorStaffRole(managerEmail, staffId, request);

        assertThat(actual).isSameAs(expected);
        assertThat(user.getRoles()).extracting(Role::getRoleName)
                .contains("COORDINATOR")
                .doesNotContain("VENDOR_STAFF");
        verify(userRepository).save(user);
    }

    @Test
    void updateVendorStaffRole_rejectsStaffFromAnotherVendor() {
        Vendor anotherVendor = new Vendor();
        anotherVendor.setVendorId(UUID.randomUUID());
        staff.setVendor(anotherVendor);

        when(vendorRepository.findByManager_Email(managerEmail)).thenReturn(Optional.of(vendor));
        when(vendorStaffRepository.findById(staffId)).thenReturn(Optional.of(staff));

        assertThatThrownBy(() -> service.updateVendorStaffRole(
                managerEmail, staffId, request(VendorStaffRole.COORDINATOR)))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.UNAUTHORIZED_STAFF_ACCESS);

        verify(userRepository, never()).save(user);
    }

    @Test
    void updateVendorStaffRole_rejectsDowngradeWhenCoordinatorHasActiveSchedule() {
        user.getRoles().clear();
        user.getRoles().add(role(VendorStaffRole.COORDINATOR));

        when(vendorRepository.findByManager_Email(managerEmail)).thenReturn(Optional.of(vendor));
        when(vendorStaffRepository.findById(staffId)).thenReturn(Optional.of(staff));
        when(coordinatorScheduleRepository.countActiveOrUpcomingSchedules(
                org.mockito.ArgumentMatchers.eq(user.getUserId()), anyCollection(), any())).thenReturn(1L);

        assertThatThrownBy(() -> service.updateVendorStaffRole(
                managerEmail, staffId, request(VendorStaffRole.VENDOR_STAFF)))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COORDINATOR_HAS_ACTIVE_SCHEDULES);

        verify(userRepository, never()).save(user);
    }

    private VendorStaffRoleUpdateRequest request(VendorStaffRole role) {
        VendorStaffRoleUpdateRequest request = new VendorStaffRoleUpdateRequest();
        request.setRole(role);
        return request;
    }

    private Role role(VendorStaffRole roleName) {
        Role role = new Role();
        role.setRoleName(roleName.name());
        return role;
    }
}
