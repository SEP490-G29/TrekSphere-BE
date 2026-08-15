package com.sep.treksphere.service.impl;

import com.sep.treksphere.dto.request.AssignCoordinatorRequest;
import com.sep.treksphere.dto.response.TourSessionSummaryResponse;
import com.sep.treksphere.entity.Role;
import com.sep.treksphere.entity.Tour;
import com.sep.treksphere.entity.TourSchedule;
import com.sep.treksphere.entity.TourSession;
import com.sep.treksphere.entity.User;
import com.sep.treksphere.entity.Vendor;
import com.sep.treksphere.entity.VendorStaff;
import com.sep.treksphere.enums.logistics.EquipmentReturnStatus;
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
import com.sep.treksphere.repository.UserRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
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
    @Mock private UserRepository userRepository;
    @Mock private TourSessionMapper tourSessionMapper;
    @Mock private SessionEquipmentMapper sessionEquipmentMapper;

    @InjectMocks private LogisticsAllocationServiceImpl service;

    @Test
    void getSessionBySchedule_returnsSessionOwnedByManagerVendor() {
        UUID scheduleId = UUID.randomUUID();
        UUID managerId = UUID.randomUUID();
        UUID vendorId = UUID.randomUUID();

        Vendor vendor = new Vendor();
        vendor.setVendorId(vendorId);
        Tour tour = new Tour();
        tour.setVendor(vendor);
        TourSchedule tourSchedule = new TourSchedule();
        tourSchedule.setScheduleId(scheduleId);
        tourSchedule.setTour(tour);
        TourSession session = new TourSession();
        session.setTourSchedule(tourSchedule);

        TourSessionSummaryResponse expected = TourSessionSummaryResponse.builder()
                .sessionId(UUID.randomUUID())
                .build();
        when(tourSessionRepository.findByTourSchedule_ScheduleIdAndIsDeletedFalse(scheduleId))
                .thenReturn(Optional.of(session));
        when(vendorRepository.findByManager_UserId(managerId)).thenReturn(Optional.of(vendor));
        when(tourSessionMapper.toSummaryResponse(session)).thenReturn(expected);

        TourSessionSummaryResponse result = service.getSessionBySchedule(scheduleId, managerId);

        assertThat(result).isSameAs(expected);
    }

    @Test
    void getSessionBySchedule_rejectsScheduleOwnedByAnotherVendor() {
        UUID scheduleId = UUID.randomUUID();
        UUID managerId = UUID.randomUUID();

        Vendor owner = new Vendor();
        owner.setVendorId(UUID.randomUUID());
        Vendor requester = new Vendor();
        requester.setVendorId(UUID.randomUUID());
        Tour tour = new Tour();
        tour.setVendor(owner);
        TourSchedule tourSchedule = new TourSchedule();
        tourSchedule.setTour(tour);
        TourSession session = new TourSession();
        session.setTourSchedule(tourSchedule);

        when(tourSessionRepository.findByTourSchedule_ScheduleIdAndIsDeletedFalse(scheduleId))
                .thenReturn(Optional.of(session));
        when(vendorRepository.findByManager_UserId(managerId)).thenReturn(Optional.of(requester));

        assertThatThrownBy(() -> service.getSessionBySchedule(scheduleId, managerId))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TOUR_NOT_BELONG_TO_VENDOR);

        verify(tourSessionMapper, never()).toSummaryResponse(session);
    }

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

    @Test
    void returnEquipment_submitsReportWithoutUpdatingInventory() {
        UUID sessionEqId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID vendorId = UUID.randomUUID();

        Vendor vendor = new Vendor();
        vendor.setVendorId(vendorId);

        Tour tour = new Tour();
        tour.setVendor(vendor);

        TourSchedule tourSchedule = new TourSchedule();
        tourSchedule.setTour(tour);

        TourSession session = new TourSession();
        session.setTourSchedule(tourSchedule);
        session.setStatus(TourSessionStatus.COMPLETED);

        com.sep.treksphere.entity.VendorEquipment vendorEq = new com.sep.treksphere.entity.VendorEquipment();
        vendorEq.setTotalQuantity(100);

        com.sep.treksphere.entity.SessionEquipment sessionEq = new com.sep.treksphere.entity.SessionEquipment();
        sessionEq.setSessionEquipmentId(sessionEqId);
        sessionEq.setTourSession(session);
        sessionEq.setEquipment(vendorEq);
        sessionEq.setQuantity(10);
        sessionEq.setReturnStatus(EquipmentReturnStatus.NOT_RETURNED);

        com.sep.treksphere.dto.request.ReturnEquipmentRequest request = new com.sep.treksphere.dto.request.ReturnEquipmentRequest();
        request.setReturnedQuantity(8);
        request.setMissingQuantity(2);
        request.setNote("2 items damaged");

        User user = new User();
        user.setUserId(userId);

        when(sessionEquipmentRepository.findById(sessionEqId)).thenReturn(Optional.of(sessionEq));
        when(vendorRepository.findByManager_UserId(userId)).thenReturn(Optional.of(vendor));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        service.returnEquipment(sessionEqId, request, userId);

        assertThat(sessionEq.getReturnStatus()).isEqualTo(EquipmentReturnStatus.PENDING_CONFIRMATION);
        assertThat(sessionEq.getReturnedQuantity()).isEqualTo(8);
        assertThat(sessionEq.getMissingQuantity()).isEqualTo(2);
        // Total quantity in warehouse should NOT be updated yet!
        assertThat(vendorEq.getTotalQuantity()).isEqualTo(100);
        verify(sessionEquipmentRepository).save(sessionEq);
        verify(vendorEquipmentRepository, never()).save(vendorEq);
    }

    @Test
    void confirmEquipmentReturn_updatesInventoryAndConfirms() {
        UUID sessionEqId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID vendorId = UUID.randomUUID();

        Vendor vendor = new Vendor();
        vendor.setVendorId(vendorId);

        Tour tour = new Tour();
        tour.setVendor(vendor);

        TourSchedule tourSchedule = new TourSchedule();
        tourSchedule.setTour(tour);

        TourSession session = new TourSession();
        session.setTourSchedule(tourSchedule);
        session.setStatus(TourSessionStatus.COMPLETED);

        com.sep.treksphere.entity.VendorEquipment vendorEq = new com.sep.treksphere.entity.VendorEquipment();
        vendorEq.setTotalQuantity(100);

        com.sep.treksphere.entity.SessionEquipment sessionEq = new com.sep.treksphere.entity.SessionEquipment();
        sessionEq.setSessionEquipmentId(sessionEqId);
        sessionEq.setTourSession(session);
        sessionEq.setEquipment(vendorEq);
        sessionEq.setQuantity(10);
        sessionEq.setReturnedQuantity(8);
        sessionEq.setMissingQuantity(2);
        sessionEq.setReturnStatus(EquipmentReturnStatus.PENDING_CONFIRMATION);

        User user = new User();
        user.setUserId(userId);

        when(sessionEquipmentRepository.findById(sessionEqId)).thenReturn(Optional.of(sessionEq));
        when(vendorRepository.findByManager_UserId(userId)).thenReturn(Optional.of(vendor));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        service.confirmEquipmentReturn(sessionEqId, userId);

        assertThat(sessionEq.getReturnStatus()).isEqualTo(EquipmentReturnStatus.CONFIRMED);
        assertThat(sessionEq.getConfirmedBy()).isEqualTo(user);
        // Total quantity in warehouse MUST be increased by returnedQuantity (8) -> 108
        assertThat(vendorEq.getTotalQuantity()).isEqualTo(108);
        verify(vendorEquipmentRepository).save(vendorEq);
        verify(sessionEquipmentRepository).save(sessionEq);
    }
}
