package com.sep.treksphere.matching.service;

import com.sep.treksphere.common.exception.AppException;
import com.sep.treksphere.common.exception.ErrorCode;
import com.sep.treksphere.matching.dto.request.CreateSosAlertRequest;
import com.sep.treksphere.matching.dto.response.SosAlertResponse;
import com.sep.treksphere.matching.entity.GroupTrip;
import com.sep.treksphere.matching.entity.MatchingGroup;
import com.sep.treksphere.matching.entity.MatchingMember;
import com.sep.treksphere.matching.entity.SosAlert;
import com.sep.treksphere.matching.enums.GroupTripStatus;
import com.sep.treksphere.matching.enums.IncidentType;
import com.sep.treksphere.matching.enums.JoinStatus;
import com.sep.treksphere.matching.enums.MatchingRole;
import com.sep.treksphere.matching.enums.SosAlertStatus;
import com.sep.treksphere.matching.repository.GroupTripRepository;
import com.sep.treksphere.matching.repository.MatchingMemberRepository;
import com.sep.treksphere.matching.repository.SosAlertRepository;
import com.sep.treksphere.matching.service.impl.SosAlertServiceImpl;
import com.sep.treksphere.notification.enums.NotificationEventType;
import com.sep.treksphere.notification.service.NotificationService;
import com.sep.treksphere.notification.enums.ReferenceType;
import com.sep.treksphere.user.entity.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SosAlertServiceImplTest {

    @Mock
    private SosAlertRepository sosAlertRepository;

    @Mock
    private GroupTripRepository groupTripRepository;

    @Mock
    private MatchingMemberRepository matchingMemberRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private SosAlertServiceImpl sosAlertService;

    private UUID groupId;
    private UUID tripId;
    private User sender;
    private User leader;
    private User otherUser;
    private GroupTrip trip;
    private MatchingMember senderMember;
    private MatchingMember leaderMember;

    @BeforeEach
    void setUp() {
        // registerSynchronization(...) trong broadcastAfterCommit() yêu cầu synchronization
        // đang active — thật ra do @Transactional cung cấp, ở đây khởi tạo thủ công vì test
        // không chạy trong context Spring.
        TransactionSynchronizationManager.initSynchronization();

        groupId = UUID.randomUUID();
        tripId = UUID.randomUUID();

        sender = new User();
        sender.setUserId(UUID.randomUUID());
        sender.setFullName("Sender Nam");

        leader = new User();
        leader.setUserId(UUID.randomUUID());
        leader.setFullName("Leader Huy");

        otherUser = new User();
        otherUser.setUserId(UUID.randomUUID());
        otherUser.setFullName("Other Lan");

        MatchingGroup group = new MatchingGroup();
        group.setMatchingGroupId(groupId);

        trip = new GroupTrip();
        trip.setGroupTripId(tripId);
        trip.setMatchingGroup(group);
        trip.setStatus(GroupTripStatus.IN_PROGRESS);

        senderMember = new MatchingMember();
        senderMember.setMatchingGroup(group);
        senderMember.setUser(sender);
        senderMember.setRole(MatchingRole.MEMBER);
        senderMember.setStatus(JoinStatus.ACCEPTED);

        leaderMember = new MatchingMember();
        leaderMember.setMatchingGroup(group);
        leaderMember.setUser(leader);
        leaderMember.setRole(MatchingRole.LEADER);
        leaderMember.setStatus(JoinStatus.ACCEPTED);
    }

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    private CreateSosAlertRequest sampleRequest(String idempotencyKey) {
        return CreateSosAlertRequest.builder()
                .incidentTypeCode(IncidentType.INJURY)
                .message("Trẹo chân")
                .idempotencyKey(idempotencyKey)
                .build();
    }

    private SosAlert newOpenAlert(String idempotencyKey) {
        SosAlert alert = new SosAlert();
        alert.setSosAlertId(UUID.randomUUID());
        alert.setGroupTrip(trip);
        alert.setSender(sender);
        alert.setIncidentTypeCode(IncidentType.INJURY);
        alert.setStatus(SosAlertStatus.OPEN);
        alert.setIdempotencyKey(idempotencyKey);
        return alert;
    }

    @Test
    @DisplayName("Happy path: create (OPEN) -> resolve bởi Leader (RESOLVED)")
    void createThenResolve_HappyPath() {
        when(matchingMemberRepository.findByGroupIdAndUserId(groupId, sender.getUserId()))
                .thenReturn(Optional.of(senderMember));
        when(groupTripRepository.findByMatchingGroup_MatchingGroupId(groupId)).thenReturn(Optional.of(trip));
        when(sosAlertRepository.findByGroupTrip_GroupTripIdAndSender_UserIdAndIdempotencyKeyAndIsDeletedFalse(
                tripId, sender.getUserId(), "key-1")).thenReturn(Optional.empty());
        when(matchingMemberRepository.findActiveMembers(groupId, JoinStatus.ACCEPTED))
                .thenReturn(List.of(senderMember, leaderMember));

        SosAlert savedAlert = newOpenAlert("key-1");
        when(sosAlertRepository.save(any(SosAlert.class))).thenReturn(savedAlert);

        SosAlertResponse createResponse = sosAlertService.createAlert(groupId, sampleRequest("key-1"), sender.getUserId());

        assertThat(createResponse.getStatus()).isEqualTo(SosAlertStatus.OPEN);
        verify(notificationService).notify(
                eq(List.of(leader.getUserId())),
                eq(NotificationEventType.SOS_ALERT_RAISED),
                eq(ReferenceType.SOS),
                eq(savedAlert.getSosAlertId()),
                anyString(),
                eq(sender.getFullName()), eq(IncidentType.INJURY.getLabel()));
        triggerAfterCommitAndReset();
        verify(messagingTemplate).convertAndSend("/topic/matching-groups/" + groupId + "/sos", createResponse);

        when(sosAlertRepository.findByIdForUpdate(savedAlert.getSosAlertId())).thenReturn(Optional.of(savedAlert));
        when(matchingMemberRepository.findByGroupIdAndUserId(groupId, leader.getUserId()))
                .thenReturn(Optional.of(leaderMember));
        when(sosAlertRepository.save(savedAlert)).thenReturn(savedAlert);

        SosAlertResponse resolveResponse = sosAlertService.resolve(groupId, savedAlert.getSosAlertId(), leader.getUserId());

        assertThat(resolveResponse.getStatus()).isEqualTo(SosAlertStatus.RESOLVED);
        assertThat(resolveResponse.getResolvedById()).isEqualTo(leader.getUserId());
        verify(notificationService).notify(
                eq(List.of(sender.getUserId())),
                eq(NotificationEventType.SOS_ALERT_RESOLVED),
                eq(ReferenceType.SOS),
                eq(savedAlert.getSosAlertId()),
                anyString(),
                eq(leader.getFullName()));
        triggerAfterCommitAndReset();
        verify(messagingTemplate).convertAndSend("/topic/matching-groups/" + groupId + "/sos", resolveResponse);
    }

    private void triggerAfterCommitAndReset() {
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(org.springframework.transaction.support.TransactionSynchronization::afterCommit);
        TransactionSynchronizationManager.clearSynchronization();
        TransactionSynchronizationManager.initSynchronization();
    }

    @Test
    @DisplayName("resolve bởi đúng Sender của alert (không phải Leader) -> OK")
    void resolve_BySenderOfAlert_Succeeds() {
        SosAlert alert = newOpenAlert("key-1");
        when(sosAlertRepository.findByIdForUpdate(alert.getSosAlertId())).thenReturn(Optional.of(alert));
        when(sosAlertRepository.save(alert)).thenReturn(alert);
        when(matchingMemberRepository.findActiveMembers(groupId, JoinStatus.ACCEPTED))
                .thenReturn(List.of(senderMember, leaderMember));

        SosAlertResponse response = sosAlertService.resolve(groupId, alert.getSosAlertId(), sender.getUserId());

        assertThat(response.getStatus()).isEqualTo(SosAlertStatus.RESOLVED);
        assertThat(response.getResolvedById()).isEqualTo(sender.getUserId());
    }

    @Test
    @DisplayName("createAlert khi Trip không IN_PROGRESS -> SESSION_FOR_SOS_NOT_ACTIVE")
    void createAlert_TripNotInProgress_ThrowsError() {
        trip.setStatus(GroupTripStatus.PLANNED);
        when(matchingMemberRepository.findByGroupIdAndUserId(groupId, sender.getUserId()))
                .thenReturn(Optional.of(senderMember));
        when(groupTripRepository.findByMatchingGroup_MatchingGroupId(groupId)).thenReturn(Optional.of(trip));

        assertThatThrownBy(() -> sosAlertService.createAlert(groupId, sampleRequest("key-1"), sender.getUserId()))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SESSION_FOR_SOS_NOT_ACTIVE);

        verify(sosAlertRepository, never()).save(any());
    }

    @Test
    @DisplayName("createAlert khi caller không phải ACCEPTED member -> UNAUTHORIZED_SOS_ALERT")
    void createAlert_CallerNotActiveMember_ThrowsError() {
        when(matchingMemberRepository.findByGroupIdAndUserId(groupId, otherUser.getUserId()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> sosAlertService.createAlert(groupId, sampleRequest("key-1"), otherUser.getUserId()))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.UNAUTHORIZED_SOS_ALERT);

        verify(sosAlertRepository, never()).save(any());
    }

    @Test
    @DisplayName("createAlert retry cùng idempotencyKey -> trả lại bản ghi cũ, không tạo mới, không notify lại")
    void createAlert_DuplicateIdempotencyKey_ReturnsExistingWithoutSideEffects() {
        when(matchingMemberRepository.findByGroupIdAndUserId(groupId, sender.getUserId()))
                .thenReturn(Optional.of(senderMember));
        when(groupTripRepository.findByMatchingGroup_MatchingGroupId(groupId)).thenReturn(Optional.of(trip));

        SosAlert existing = newOpenAlert("key-1");
        when(sosAlertRepository.findByGroupTrip_GroupTripIdAndSender_UserIdAndIdempotencyKeyAndIsDeletedFalse(
                tripId, sender.getUserId(), "key-1")).thenReturn(Optional.of(existing));

        SosAlertResponse response = sosAlertService.createAlert(groupId, sampleRequest("key-1"), sender.getUserId());

        assertThat(response.getSosAlertId()).isEqualTo(existing.getSosAlertId());
        verify(sosAlertRepository, never()).save(any());
        verify(notificationService, never()).notify(anyList(), any(), any(), any(), anyString(), any());
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }

    @Test
    @DisplayName("createAlert khi sender đang có alert OPEN khác -> SOS_ALERT_SENDER_HAS_ACTIVE_ALERT")
    void createAlert_SenderHasActiveAlert_ThrowsError() {
        when(matchingMemberRepository.findByGroupIdAndUserId(groupId, sender.getUserId()))
                .thenReturn(Optional.of(senderMember));
        when(groupTripRepository.findByMatchingGroup_MatchingGroupId(groupId)).thenReturn(Optional.of(trip));
        when(sosAlertRepository.findByGroupTrip_GroupTripIdAndSender_UserIdAndIdempotencyKeyAndIsDeletedFalse(
                tripId, sender.getUserId(), "key-2")).thenReturn(Optional.empty());
        when(sosAlertRepository.existsByGroupTrip_GroupTripIdAndSender_UserIdAndStatusAndIsDeletedFalse(
                tripId, sender.getUserId(), SosAlertStatus.OPEN)).thenReturn(true);

        assertThatThrownBy(() -> sosAlertService.createAlert(groupId, sampleRequest("key-2"), sender.getUserId()))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SOS_ALERT_SENDER_HAS_ACTIVE_ALERT);

        verify(sosAlertRepository, never()).save(any());
    }

    @Test
    @DisplayName("createAlert khi sender KHÔNG còn alert OPEN nào (đã resolve hết) -> tạo mới bình thường")
    void createAlert_SenderHasNoActiveAlert_Succeeds() {
        when(matchingMemberRepository.findByGroupIdAndUserId(groupId, sender.getUserId()))
                .thenReturn(Optional.of(senderMember));
        when(groupTripRepository.findByMatchingGroup_MatchingGroupId(groupId)).thenReturn(Optional.of(trip));
        when(sosAlertRepository.findByGroupTrip_GroupTripIdAndSender_UserIdAndIdempotencyKeyAndIsDeletedFalse(
                tripId, sender.getUserId(), "key-2")).thenReturn(Optional.empty());
        when(sosAlertRepository.existsByGroupTrip_GroupTripIdAndSender_UserIdAndStatusAndIsDeletedFalse(
                tripId, sender.getUserId(), SosAlertStatus.OPEN)).thenReturn(false);
        when(matchingMemberRepository.findActiveMembers(groupId, JoinStatus.ACCEPTED))
                .thenReturn(List.of(senderMember, leaderMember));
        when(sosAlertRepository.save(any(SosAlert.class))).thenReturn(newOpenAlert("key-2"));

        SosAlertResponse response = sosAlertService.createAlert(groupId, sampleRequest("key-2"), sender.getUserId());

        assertThat(response.getStatus()).isEqualTo(SosAlertStatus.OPEN);
        verify(sosAlertRepository).save(any(SosAlert.class));
    }

    @Test
    @DisplayName("resolve bởi member khác (không phải Sender, không phải Leader) -> UNAUTHORIZED_RESOLVE_SOS, alert giữ nguyên OPEN")
    void resolve_ByUnrelatedMember_ThrowsError() {
        SosAlert alert = newOpenAlert("key-1");
        when(sosAlertRepository.findByIdForUpdate(alert.getSosAlertId())).thenReturn(Optional.of(alert));

        MatchingMember unrelatedMember = new MatchingMember();
        unrelatedMember.setMatchingGroup(trip.getMatchingGroup());
        unrelatedMember.setUser(otherUser);
        unrelatedMember.setRole(MatchingRole.MEMBER);
        unrelatedMember.setStatus(JoinStatus.ACCEPTED);
        when(matchingMemberRepository.findByGroupIdAndUserId(groupId, otherUser.getUserId()))
                .thenReturn(Optional.of(unrelatedMember));

        assertThatThrownBy(() -> sosAlertService.resolve(groupId, alert.getSosAlertId(), otherUser.getUserId()))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.UNAUTHORIZED_RESOLVE_SOS);

        assertThat(alert.getStatus()).isEqualTo(SosAlertStatus.OPEN);
        verify(sosAlertRepository, never()).save(any());
    }

    @Test
    @DisplayName("resolve trên alert đã RESOLVED -> SOS_ALERT_ALREADY_RESOLVED, kể cả khi actor hợp lệ")
    void resolve_AlreadyResolvedAlert_ThrowsError() {
        SosAlert alert = newOpenAlert("key-1");
        alert.setStatus(SosAlertStatus.RESOLVED);
        alert.setResolvedBy(leader);
        when(sosAlertRepository.findByIdForUpdate(alert.getSosAlertId())).thenReturn(Optional.of(alert));

        assertThatThrownBy(() -> sosAlertService.resolve(groupId, alert.getSosAlertId(), sender.getUserId()))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SOS_ALERT_ALREADY_RESOLVED);

        verify(sosAlertRepository, never()).save(any());
    }

    @Test
    @DisplayName("resolve trên alert thuộc group khác -> SOS_ALERT_NOT_FOUND (chống IDOR)")
    void resolve_AlertBelongsToDifferentGroup_ThrowsNotFound() {
        SosAlert alert = newOpenAlert("key-1");
        UUID anotherGroupId = UUID.randomUUID();
        when(sosAlertRepository.findByIdForUpdate(alert.getSosAlertId())).thenReturn(Optional.of(alert));

        assertThatThrownBy(() -> sosAlertService.resolve(anotherGroupId, alert.getSosAlertId(), sender.getUserId()))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SOS_ALERT_NOT_FOUND);

        verify(sosAlertRepository, never()).save(any());
    }
}
