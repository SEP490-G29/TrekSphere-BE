package com.sep.treksphere.service.impl;

import com.sep.treksphere.dto.request.ParticipantAttendanceItem;
import com.sep.treksphere.dto.request.TourSessionAttendanceRequest;
import com.sep.treksphere.entity.BookingParticipant;
import com.sep.treksphere.entity.CoordinatorSchedule;
import com.sep.treksphere.entity.Role;
import com.sep.treksphere.entity.SosAlert;
import com.sep.treksphere.entity.Tour;
import com.sep.treksphere.entity.TourSchedule;
import com.sep.treksphere.entity.TourSession;
import com.sep.treksphere.entity.User;
import com.sep.treksphere.entity.Vendor;
import com.sep.treksphere.enums.booking.BookingStatus;
import com.sep.treksphere.enums.tour.AttendanceType;
import com.sep.treksphere.entity.SessionEquipment;
import com.sep.treksphere.enums.tour.SessionCheckpointLogStatus;
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
import static org.mockito.Mockito.lenient;
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
    @Mock
    private com.sep.treksphere.service.TrackingRevisionService trackingRevisionService;

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
    void getTourSessionSosStatus_returnsNoAlertWhenSessionHasNoSosHistory() {
        UUID coordinatorId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        stubAuthorizedSession(coordinatorId, sessionId, UUID.randomUUID(), TourSessionStatus.IN_PROGRESS);
        when(sosAlertRepository
                .findFirstByTourSession_TourSessionIdAndStatusAndIsDeletedFalseOrderByCreatedAtDesc(
                        sessionId, SosAlertStatus.PENDING))
                .thenReturn(Optional.empty());
        when(sosAlertRepository.findFirstByTourSession_TourSessionIdAndIsDeletedFalseOrderByCreatedAtDesc(sessionId))
                .thenReturn(Optional.empty());

        var response = trackingService.getTourSessionSosStatus(coordinatorId, sessionId);

        assertThat(response.isHasSosAlert()).isFalse();
        assertThat(response.isHasActiveSosAlert()).isFalse();
        assertThat(response.isResolved()).isFalse();
        assertThat(response.getStatus()).isNull();
        assertThat(response.getSosAlert()).isNull();
    }

    @Test
    void getTourSessionSosStatus_prioritizesPendingAlert() {
        UUID coordinatorId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        TourSession session = stubAuthorizedSession(
                coordinatorId, sessionId, UUID.randomUUID(), TourSessionStatus.IN_PROGRESS);
        User sender = new User();
        sender.setUserId(coordinatorId);
        sender.setFullName("Coordinator");
        SosAlert pendingAlert = new SosAlert();
        pendingAlert.setSosAlertId(UUID.randomUUID());
        pendingAlert.setTourSession(session);
        pendingAlert.setSender(sender);
        pendingAlert.setStatus(SosAlertStatus.PENDING);

        when(sosAlertRepository
                .findFirstByTourSession_TourSessionIdAndStatusAndIsDeletedFalseOrderByCreatedAtDesc(
                        sessionId, SosAlertStatus.PENDING))
                .thenReturn(Optional.of(pendingAlert));

        var response = trackingService.getTourSessionSosStatus(coordinatorId, sessionId);

        assertThat(response.isHasSosAlert()).isTrue();
        assertThat(response.isHasActiveSosAlert()).isTrue();
        assertThat(response.isResolved()).isFalse();
        assertThat(response.getStatus()).isEqualTo(SosAlertStatus.PENDING);
        assertThat(response.getSosAlert().getSosAlertId()).isEqualTo(pendingAlert.getSosAlertId());
        verify(sosAlertRepository, never())
                .findFirstByTourSession_TourSessionIdAndIsDeletedFalseOrderByCreatedAtDesc(sessionId);
    }

    @Test
    void getTourSessionSosStatus_returnsResolvedLatestAlertWhenNoPendingAlertExists() {
        UUID coordinatorId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        TourSession session = stubAuthorizedSession(
                coordinatorId, sessionId, UUID.randomUUID(), TourSessionStatus.IN_PROGRESS);
        User sender = new User();
        sender.setUserId(coordinatorId);
        sender.setFullName("Coordinator");
        SosAlert resolvedAlert = new SosAlert();
        resolvedAlert.setSosAlertId(UUID.randomUUID());
        resolvedAlert.setTourSession(session);
        resolvedAlert.setSender(sender);
        resolvedAlert.setStatus(SosAlertStatus.RESOLVED);

        when(sosAlertRepository
                .findFirstByTourSession_TourSessionIdAndStatusAndIsDeletedFalseOrderByCreatedAtDesc(
                        sessionId, SosAlertStatus.PENDING))
                .thenReturn(Optional.empty());
        when(sosAlertRepository.findFirstByTourSession_TourSessionIdAndIsDeletedFalseOrderByCreatedAtDesc(sessionId))
                .thenReturn(Optional.of(resolvedAlert));

        var response = trackingService.getTourSessionSosStatus(coordinatorId, sessionId);

        assertThat(response.isHasSosAlert()).isTrue();
        assertThat(response.isHasActiveSosAlert()).isFalse();
        assertThat(response.isResolved()).isTrue();
        assertThat(response.getStatus()).isEqualTo(SosAlertStatus.RESOLVED);
        assertThat(response.getSosAlert().getSosAlertId()).isEqualTo(resolvedAlert.getSosAlertId());
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
                scheduleId, BookingStatus.IN_PROGRESS
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
        stubAuthorizedSession(coordinatorId, sessionId, scheduleId, TourSessionStatus.PENDING);
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

    @Test
    void endSession_RejectsWhenEndAttendanceIncomplete() {
        UUID coordinatorId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID scheduleId = UUID.randomUUID();
        stubAuthorizedSession(coordinatorId, sessionId, scheduleId, TourSessionStatus.IN_PROGRESS);

        BookingParticipant p = participant(UUID.randomUUID());
        p.setIsPresentStart(true);
        p.setIsPresentEnd(false);
        when(bookingParticipantRepository.findActiveParticipantsByScheduleId(scheduleId, BookingStatus.IN_PROGRESS))
                .thenReturn(List.of(p));

        assertThatThrownBy(() -> trackingService.endSession(coordinatorId, sessionId))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.END_ATTENDANCE_INCOMPLETE);
    }

    @Test
    void endSession_RejectsWhenEquipmentNotReady() {
        UUID coordinatorId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID scheduleId = UUID.randomUUID();
        stubAuthorizedSession(coordinatorId, sessionId, scheduleId, TourSessionStatus.IN_PROGRESS);

        BookingParticipant p = participant(UUID.randomUUID());
        p.setIsPresentStart(true);
        p.setIsPresentEnd(true);
        when(bookingParticipantRepository.findActiveParticipantsByScheduleId(scheduleId, BookingStatus.IN_PROGRESS))
                .thenReturn(List.of(p));

        SessionEquipment eq = new SessionEquipment();
        eq.setIsChecked(false);
        when(sessionEquipmentRepository.findByTourSession_TourSessionIdAndIsDeletedFalse(sessionId))
                .thenReturn(List.of(eq));

        assertThatThrownBy(() -> trackingService.endSession(coordinatorId, sessionId))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SESSION_EQUIPMENT_NOT_READY);
    }

    @Test
    void endSession_SuccessWhenAttendanceAndEquipmentComplete() {
        UUID coordinatorId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID scheduleId = UUID.randomUUID();
        TourSession session = stubAuthorizedSession(coordinatorId, sessionId, scheduleId, TourSessionStatus.IN_PROGRESS);

        BookingParticipant p = participant(UUID.randomUUID());
        p.setIsPresentStart(true);
        p.setIsPresentEnd(true);
        when(bookingParticipantRepository.findActiveParticipantsByScheduleId(scheduleId, BookingStatus.IN_PROGRESS))
                .thenReturn(List.of(p));

        SessionEquipment eq = new SessionEquipment();
        eq.setIsChecked(true);
        when(sessionEquipmentRepository.findByTourSession_TourSessionIdAndIsDeletedFalse(sessionId))
                .thenReturn(List.of(eq));

        when(sosAlertRepository.findFirstByTourSession_TourSessionIdAndStatusAndIsDeletedFalseOrderByCreatedAtDesc(
                sessionId, SosAlertStatus.PENDING))
                .thenReturn(Optional.empty());

        when(sessionCheckpointLogRepository.findByTourSession_TourSessionIdAndStatusAndIsDeletedFalseOrderByCheckpoint_CheckpointOrderAsc(
                sessionId, SessionCheckpointLogStatus.PENDING))
                .thenReturn(List.of());

        when(bookingRepository.findBySchedule_ScheduleIdAndBookingStatusAndIsDeletedFalse(
                scheduleId, BookingStatus.IN_PROGRESS))
                .thenReturn(List.of());

        var response = trackingService.endSession(coordinatorId, sessionId);

        assertThat(response.getTourSessionId()).isEqualTo(sessionId);
        assertThat(response.getStatus()).isEqualTo(TourSessionStatus.COMPLETED);
        assertThat(session.getStatus()).isEqualTo(TourSessionStatus.COMPLETED);
    }

    private TourSession stubAuthorizedSession(
            UUID coordinatorId,
            UUID sessionId,
            UUID scheduleId,
            TourSessionStatus status
    ) {
        TourSchedule tourSchedule = new TourSchedule();
        tourSchedule.setScheduleId(scheduleId);
        Tour tour = new Tour();
        tour.setTourName("Test Tour");
        tourSchedule.setTour(tour);

        TourSession tourSession = new TourSession();
        tourSession.setTourSessionId(sessionId);
        tourSession.setTourSchedule(tourSchedule);
        tourSession.setStatus(status);

        CoordinatorSchedule coordinatorSchedule = new CoordinatorSchedule();
        coordinatorSchedule.setIsCancelled(false);
        coordinatorSchedule.setIsLead(true);

        lenient().when(tourSessionRepository.findByTourSessionIdAndIsDeletedFalse(sessionId))
                .thenReturn(Optional.of(tourSession));
        lenient().when(tourSessionRepository.findByIdForUpdate(sessionId))
                .thenReturn(Optional.of(tourSession));
        lenient().when(coordinatorScheduleRepository
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
