package com.sep.treksphere.service.impl;

import com.sep.treksphere.dto.request.ParticipantAttendanceItem;
import com.sep.treksphere.dto.request.TourSessionAttendanceRequest;
import com.sep.treksphere.entity.BookingParticipant;
import com.sep.treksphere.entity.CoordinatorSchedule;
import com.sep.treksphere.entity.Role;
import com.sep.treksphere.entity.TourSchedule;
import com.sep.treksphere.entity.TourSession;
import com.sep.treksphere.entity.User;
import com.sep.treksphere.entity.Vendor;
import com.sep.treksphere.enums.booking.BookingStatus;
import com.sep.treksphere.enums.tour.AttendanceType;
import com.sep.treksphere.enums.tour.SosAlertStatus;
import com.sep.treksphere.enums.tour.TourSessionStatus;
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

import java.util.List;
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

    @Test
    void recordAttendance_AllowsStartAttendanceWhileSessionIsPending() {
        UUID coordinatorId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID scheduleId = UUID.randomUUID();
        UUID participantId = UUID.randomUUID();
        TourSession session = stubAuthorizedSession(
                coordinatorId, sessionId, scheduleId, TourSessionStatus.PENDING
        );
        BookingParticipant participant = participant(participantId);
        when(bookingParticipantRepository.findActiveParticipantsByScheduleId(
                scheduleId, BookingStatus.CONFIRMED
        )).thenReturn(List.of(participant));

        TourSessionAttendanceRequest request = attendanceRequest(
                AttendanceType.START,
                List.of(attendanceItem(participantId, true))
        );

        var response = trackingService.recordAttendance(coordinatorId, sessionId, request);

        assertThat(response.getTourSessionId()).isEqualTo(session.getTourSessionId());
        assertThat(response.getAttendanceType()).isEqualTo(AttendanceType.START);
        assertThat(participant.getIsPresentStart()).isTrue();
        assertThat(participant.getStartAttendedAt()).isNotNull();
        verify(bookingParticipantRepository).saveAll(List.of(participant));
    }

    @Test
    void recordAttendance_RejectsEndAttendanceWhenStartWasNotRecorded() {
        UUID coordinatorId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID scheduleId = UUID.randomUUID();
        UUID participantId = UUID.randomUUID();
        stubAuthorizedSession(coordinatorId, sessionId, scheduleId, TourSessionStatus.IN_PROGRESS);
        BookingParticipant participant = participant(participantId);
        when(bookingParticipantRepository.findActiveParticipantsByScheduleId(
                scheduleId, BookingStatus.CONFIRMED
        )).thenReturn(List.of(participant));

        TourSessionAttendanceRequest request = attendanceRequest(
                AttendanceType.END,
                List.of(attendanceItem(participantId, true))
        );

        assertThatThrownBy(() -> trackingService.recordAttendance(coordinatorId, sessionId, request))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ATTENDANCE_START_REQUIRED);

        verify(bookingParticipantRepository, never()).saveAll(anyList());
    }

    @Test
    void recordAttendance_RejectsDuplicateParticipantIds() {
        UUID coordinatorId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID scheduleId = UUID.randomUUID();
        UUID participantId = UUID.randomUUID();
        stubAuthorizedSession(coordinatorId, sessionId, scheduleId, TourSessionStatus.IN_PROGRESS);
        TourSessionAttendanceRequest request = attendanceRequest(
                AttendanceType.START,
                List.of(
                        attendanceItem(participantId, true),
                        attendanceItem(participantId, false)
                )
        );

        assertThatThrownBy(() -> trackingService.recordAttendance(coordinatorId, sessionId, request))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_PARTICIPANT_IN_ATTENDANCE);

        verifyNoInteractions(bookingParticipantRepository);
    }

    @Test
    void recordAttendance_RejectsEndAttendanceWhileSessionIsPending() {
        UUID coordinatorId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID scheduleId = UUID.randomUUID();
        stubAuthorizedSession(coordinatorId, sessionId, scheduleId, TourSessionStatus.PENDING);
        TourSessionAttendanceRequest request = attendanceRequest(
                AttendanceType.END,
                List.of(attendanceItem(UUID.randomUUID(), true))
        );

        assertThatThrownBy(() -> trackingService.recordAttendance(coordinatorId, sessionId, request))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SESSION_NOT_IN_PROGRESS);

        verifyNoInteractions(bookingParticipantRepository);
    }

    private TourSession stubAuthorizedSession(
            UUID coordinatorId,
            UUID sessionId,
            UUID scheduleId,
            TourSessionStatus status
    ) {
        TourSchedule tourSchedule = new TourSchedule();
        tourSchedule.setScheduleId(scheduleId);

        TourSession tourSession = new TourSession();
        tourSession.setTourSessionId(sessionId);
        tourSession.setTourSchedule(tourSchedule);
        tourSession.setStatus(status);

        CoordinatorSchedule coordinatorSchedule = new CoordinatorSchedule();
        coordinatorSchedule.setIsCancelled(false);

        when(tourSessionRepository.findByTourSessionIdAndIsDeletedFalse(sessionId))
                .thenReturn(Optional.of(tourSession));
        when(coordinatorScheduleRepository
                .findByTourSession_TourSessionIdAndCoordinator_UserIdAndIsDeletedFalse(
                        sessionId, coordinatorId
                )).thenReturn(Optional.of(coordinatorSchedule));
        return tourSession;
    }

    private BookingParticipant participant(UUID participantId) {
        BookingParticipant participant = new BookingParticipant();
        participant.setParticipantId(participantId);
        participant.setFullName("Test Trekker");
        return participant;
    }

    private TourSessionAttendanceRequest attendanceRequest(
            AttendanceType attendanceType,
            List<ParticipantAttendanceItem> participants
    ) {
        return TourSessionAttendanceRequest.builder()
                .attendanceType(attendanceType)
                .participants(participants)
                .build();
    }

    private ParticipantAttendanceItem attendanceItem(UUID participantId, boolean isPresent) {
        return ParticipantAttendanceItem.builder()
                .participantId(participantId)
                .isPresent(isPresent)
                .build();
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
