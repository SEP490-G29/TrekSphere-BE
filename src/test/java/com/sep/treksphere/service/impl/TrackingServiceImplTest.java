package com.sep.treksphere.service.impl;

import com.sep.treksphere.entity.Role;
import com.sep.treksphere.entity.User;
import com.sep.treksphere.entity.Vendor;
import com.sep.treksphere.enums.tour.SosAlertStatus;
import com.sep.treksphere.exception.AppException;
import com.sep.treksphere.exception.ErrorCode;
import com.sep.treksphere.repository.BookingParticipantRepository;
import com.sep.treksphere.repository.BookingRepository;
import com.sep.treksphere.repository.CoordinatorScheduleRepository;
import com.sep.treksphere.repository.SessionCheckpointLogRepository;
import com.sep.treksphere.repository.SessionEquipmentRepository;
import com.sep.treksphere.repository.SosAlertRepository;
import com.sep.treksphere.repository.TourCheckpointRepository;
import com.sep.treksphere.repository.TourSessionRepository;
import com.sep.treksphere.repository.UserRepository;
import com.sep.treksphere.repository.VendorRepository;
import com.sep.treksphere.repository.VendorStaffRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrackingServiceImplTest {

    @Mock
    private VendorRepository vendorRepository;
    @Mock
    private TourSessionRepository tourSessionRepository;
    @Mock
    private CoordinatorScheduleRepository coordinatorScheduleRepository;
    @Mock
    private TourCheckpointRepository tourCheckpointRepository;
    @Mock
    private SessionCheckpointLogRepository sessionCheckpointLogRepository;
    @Mock
    private BookingParticipantRepository bookingParticipantRepository;
    @Mock
    private SessionEquipmentRepository sessionEquipmentRepository;
    @Mock
    private VendorStaffRepository vendorStaffRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private SosAlertRepository sosAlertRepository;

    @InjectMocks
    private TrackingServiceImpl trackingService;

    @Test
    void getActiveSosAlerts_AdminCanViewAllPendingAlerts() {
        UUID userId = UUID.randomUUID();
        User admin = userWithRole(userId, "ADMIN");
        Pageable pageable = PageRequest.of(0, 20);

        when(userRepository.findById(userId)).thenReturn(Optional.of(admin));
        when(sosAlertRepository.findAlertsByStatus(SosAlertStatus.PENDING, null, pageable))
                .thenReturn(Page.empty(pageable));

        var response = trackingService.getActiveSosAlerts(userId, pageable);

        verify(sosAlertRepository).findAlertsByStatus(SosAlertStatus.PENDING, null, pageable);
        verifyNoInteractions(vendorRepository);
        assertThat(response.getContent()).isEmpty();
        assertThat(response.getPageNumber()).isZero();
        assertThat(response.getPageSize()).isEqualTo(20);
    }

    @Test
    void getActiveSosAlerts_VendorManagerOnlyViewsOwnVendorAlerts() {
        UUID userId = UUID.randomUUID();
        UUID vendorId = UUID.randomUUID();
        User manager = userWithRole(userId, "VENDOR_MANAGER");
        Vendor vendor = new Vendor();
        vendor.setVendorId(vendorId);
        Pageable pageable = PageRequest.of(0, 20);

        when(userRepository.findById(userId)).thenReturn(Optional.of(manager));
        when(vendorRepository.findByManager_UserId(userId)).thenReturn(Optional.of(vendor));
        when(sosAlertRepository.findAlertsByStatus(SosAlertStatus.PENDING, vendorId, pageable))
                .thenReturn(Page.empty(pageable));

        trackingService.getActiveSosAlerts(userId, pageable);

        verify(sosAlertRepository).findAlertsByStatus(SosAlertStatus.PENDING, vendorId, pageable);
    }

    @Test
    void getActiveSosAlerts_RejectsUnsupportedRole() {
        UUID userId = UUID.randomUUID();
        User user = userWithRole(userId, "TREKKER");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> trackingService.getActiveSosAlerts(userId, PageRequest.of(0, 20)))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCESS_DENIED);

        verifyNoInteractions(vendorRepository, sosAlertRepository);
    }

    @Test
    void getActiveSosAlerts_ReturnsVendorNotFound_WhenManagerHasNoVendor() {
        UUID userId = UUID.randomUUID();
        User manager = userWithRole(userId, "VENDOR_MANAGER");
        when(userRepository.findById(userId)).thenReturn(Optional.of(manager));
        when(vendorRepository.findByManager_UserId(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> trackingService.getActiveSosAlerts(userId, PageRequest.of(0, 20)))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VENDOR_NOT_FOUND);

        verifyNoInteractions(sosAlertRepository);
    }

    private User userWithRole(UUID userId, String roleName) {
        Role role = new Role();
        role.setRoleName(roleName);

        User user = new User();
        user.setUserId(userId);
        user.getRoles().add(role);
        return user;
    }
}
