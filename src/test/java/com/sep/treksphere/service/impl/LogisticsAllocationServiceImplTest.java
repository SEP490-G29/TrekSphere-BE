package com.sep.treksphere.service.impl;

import com.sep.treksphere.dto.request.AssignCoordinatorRequest;
import com.sep.treksphere.entity.Role;
import com.sep.treksphere.entity.Tour;
import com.sep.treksphere.entity.TourSchedule;
import com.sep.treksphere.entity.TourSession;
import com.sep.treksphere.entity.User;
import com.sep.treksphere.entity.Vendor;
import com.sep.treksphere.entity.VendorStaff;
import com.sep.treksphere.enums.tour.TourSessionStatus;
import com.sep.treksphere.exception.AppException;
import com.sep.treksphere.exception.ErrorCode;
import com.sep.treksphere.mapper.SessionEquipmentMapper;
import com.sep.treksphere.mapper.TourSessionMapper;
import com.sep.treksphere.repository.CoordinatorScheduleRepository;
import com.sep.treksphere.repository.PorterProfileRepository;
import com.sep.treksphere.repository.PorterScheduleRepository;
import com.sep.treksphere.repository.SessionEquipmentRepository;
import com.sep.treksphere.repository.TourSessionRepository;
import com.sep.treksphere.repository.VendorEquipmentRepository;
import com.sep.treksphere.repository.VendorRepository;
import com.sep.treksphere.repository.VendorStaffRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LogisticsAllocationServiceImplTest {

    @Mock private TourSessionRepository tourSessionRepository;
    @Mock private CoordinatorScheduleRepository coordinatorScheduleRepository;
    @Mock private PorterScheduleRepository porterScheduleRepository;
    @Mock private PorterProfileRepository porterProfileRepository;
    @Mock private VendorRepository vendorRepository;
    @Mock private VendorStaffRepository vendorStaffRepository;
    @Mock private VendorEquipmentRepository vendorEquipmentRepository;
    @Mock private SessionEquipmentRepository sessionEquipmentRepository;
    @Mock private TourSessionMapper tourSessionMapper;
    @Mock private SessionEquipmentMapper sessionEquipmentMapper;

    @InjectMocks private LogisticsAllocationServiceImpl service;

    @Test
    void assignCoordinator_rejectsRegularVendorStaff() {
        UUID sessionId = UUID.randomUUID();
        UUID managerId = UUID.randomUUID();
        UUID coordinatorId = UUID.randomUUID();
        UUID vendorId = UUID.randomUUID();

        Vendor vendor = new Vendor();
        vendor.setVendorId(vendorId);

        Tour tour = new Tour();
        tour.setVendor(vendor);
        TourSchedule tourSchedule = new TourSchedule();
        tourSchedule.setTour(tour);
        TourSession session = new TourSession();
        session.setTourSchedule(tourSchedule);
        session.setStatus(TourSessionStatus.PENDING);

        User regularStaffUser = new User();
        regularStaffUser.setUserId(coordinatorId);
        Role regularStaffRole = new Role();
        regularStaffRole.setRoleName("VENDOR_STAFF");
        regularStaffUser.getRoles().add(regularStaffRole);

        VendorStaff regularStaff = new VendorStaff();
        regularStaff.setVendor(vendor);
        regularStaff.setUser(regularStaffUser);

        AssignCoordinatorRequest request = new AssignCoordinatorRequest();
        request.setCoordinatorId(coordinatorId);

        when(tourSessionRepository.findByIdWithVendor(sessionId)).thenReturn(Optional.of(session));
        when(vendorRepository.findByManager_UserId(managerId)).thenReturn(Optional.of(vendor));
        when(vendorStaffRepository.findByUser_UserIdAndIsActiveTrueAndIsDeletedFalse(coordinatorId))
                .thenReturn(Optional.of(regularStaff));

        assertThatThrownBy(() -> service.assignCoordinator(sessionId, request, managerId))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COORDINATOR_NOT_FOUND);

        verify(coordinatorScheduleRepository, never())
                .existsByTourSession_TourSessionIdAndCoordinator_UserIdAndIsDeletedFalse(sessionId, coordinatorId);
    }
}
