package com.sep.treksphere.matching.service.impl;

import com.sep.treksphere.common.exception.AppException;
import com.sep.treksphere.common.exception.ErrorCode;
import com.sep.treksphere.matching.dto.request.CreateSosAlertRequest;
import com.sep.treksphere.matching.dto.response.SosAlertResponse;
import com.sep.treksphere.matching.entity.GroupTrip;
import com.sep.treksphere.matching.entity.MatchingMember;
import com.sep.treksphere.matching.entity.SosAlert;
import com.sep.treksphere.matching.enums.GroupTripStatus;
import com.sep.treksphere.matching.enums.JoinStatus;
import com.sep.treksphere.matching.enums.MatchingRole;
import com.sep.treksphere.matching.enums.SosAlertStatus;
import com.sep.treksphere.matching.repository.GroupTripRepository;
import com.sep.treksphere.matching.repository.MatchingMemberRepository;
import com.sep.treksphere.matching.repository.SosAlertRepository;
import com.sep.treksphere.matching.service.SosAlertService;
import com.sep.treksphere.notification.NotificationEventType;
import com.sep.treksphere.notification.NotificationService;
import com.sep.treksphere.notification.ReferenceType;
import com.sep.treksphere.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SosAlertServiceImpl implements SosAlertService {

    private final SosAlertRepository sosAlertRepository;
    private final GroupTripRepository groupTripRepository;
    private final MatchingMemberRepository matchingMemberRepository;
    private final NotificationService notificationService;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    @Transactional
    public SosAlertResponse createAlert(UUID groupId, CreateSosAlertRequest request, UUID currentUserId) {
        MatchingMember callerMember = requireActiveMember(groupId, currentUserId);
        GroupTrip trip = resolveGroupTripInProgress(groupId);

        Optional<SosAlert> existing = sosAlertRepository
                .findByGroupTrip_GroupTripIdAndSender_UserIdAndIdempotencyKeyAndIsDeletedFalse(
                        trip.getGroupTripId(), currentUserId, request.getIdempotencyKey());
        if (existing.isPresent()) {

            return SosAlertResponse.from(existing.get());
        }

        User sender = callerMember.getUser();

        SosAlert alert = new SosAlert();
        alert.setGroupTrip(trip);
        alert.setSender(sender);
        alert.setIncidentTypeCode(request.getIncidentTypeCode());
        alert.setLatitude(request.getLatitude());
        alert.setLongitude(request.getLongitude());
        alert.setMessage(request.getMessage());
        alert.setIdempotencyKey(request.getIdempotencyKey());
        alert.setStatus(SosAlertStatus.OPEN);
        SosAlert saved = sosAlertRepository.save(alert);

        List<UUID> recipientIds = matchingMemberRepository.findActiveMembers(groupId, JoinStatus.ACCEPTED)
                .stream()
                .map(m -> m.getUser().getUserId())
                .filter(id -> !id.equals(currentUserId))
                .toList();

        String actionUrl = "/trekker/my-groups/" + groupId;
        notificationService.notify(recipientIds, NotificationEventType.SOS_ALERT_RAISED,
                ReferenceType.SOS, saved.getSosAlertId(), actionUrl,
                sender.getFullName(), saved.getIncidentTypeCode().getLabel());

        SosAlertResponse response = SosAlertResponse.from(saved);
        broadcastAfterCommit(groupId, response);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SosAlertResponse> getActiveAlerts(UUID groupId, UUID currentUserId) {
        requireActiveMember(groupId, currentUserId);
        GroupTrip trip = groupTripRepository.findByMatchingGroup_MatchingGroupId(groupId)
                .orElseThrow(() -> new AppException(ErrorCode.GROUP_TRIP_NOT_FOUND));

        return sosAlertRepository
                .findByGroupTrip_GroupTripIdAndStatusAndIsDeletedFalseOrderByCreatedAtDesc(
                        trip.getGroupTripId(), SosAlertStatus.OPEN)
                .stream()
                .map(SosAlertResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SosAlertResponse> getAlertHistory(UUID groupId, Pageable pageable, UUID currentUserId) {
        requireActiveMember(groupId, currentUserId);
        GroupTrip trip = groupTripRepository.findByMatchingGroup_MatchingGroupId(groupId)
                .orElseThrow(() -> new AppException(ErrorCode.GROUP_TRIP_NOT_FOUND));

        return sosAlertRepository
                .findByGroupTrip_GroupTripIdAndIsDeletedFalseOrderByCreatedAtDesc(trip.getGroupTripId(), pageable)
                .map(SosAlertResponse::from);
    }

    @Override
    @Transactional
    public SosAlertResponse resolve(UUID groupId, UUID sosAlertId, UUID currentUserId) {
        SosAlert alert = lockAlertInGroupOrThrow(groupId, sosAlertId);
        User actor = requireSenderOrLeader(groupId, alert, currentUserId);
        assertTransitionAllowed(alert.getStatus());

        alert.setStatus(SosAlertStatus.RESOLVED);
        alert.setResolvedBy(actor);
        SosAlert saved = sosAlertRepository.save(alert);

        List<UUID> recipientIds = matchingMemberRepository.findActiveMembers(groupId, JoinStatus.ACCEPTED)
                .stream()
                .map(m -> m.getUser().getUserId())
                .filter(id -> !id.equals(currentUserId))
                .toList();

        String actionUrl = "/trekker/my-groups/" + groupId;
        notificationService.notify(recipientIds, NotificationEventType.SOS_ALERT_RESOLVED,
                ReferenceType.SOS, saved.getSosAlertId(), actionUrl, actor.getFullName());

        SosAlertResponse response = SosAlertResponse.from(saved);
        broadcastAfterCommit(groupId, response);
        return response;
    }

    private MatchingMember requireActiveMember(UUID groupId, UUID userId) {
        return matchingMemberRepository.findByGroupIdAndUserId(groupId, userId)
                .filter(m -> m.getStatus() == JoinStatus.ACCEPTED)
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHORIZED_SOS_ALERT));
    }

    private GroupTrip resolveGroupTripInProgress(UUID groupId) {
        GroupTrip trip = groupTripRepository.findByMatchingGroup_MatchingGroupId(groupId)
                .orElseThrow(() -> new AppException(ErrorCode.GROUP_TRIP_NOT_FOUND));
        if (trip.getStatus() != GroupTripStatus.IN_PROGRESS) {
            throw new AppException(ErrorCode.SESSION_FOR_SOS_NOT_ACTIVE);
        }
        return trip;
    }

    private SosAlert lockAlertInGroupOrThrow(UUID groupId, UUID sosAlertId) {
        SosAlert alert = sosAlertRepository.findByIdForUpdate(sosAlertId)
                .orElseThrow(() -> new AppException(ErrorCode.SOS_ALERT_NOT_FOUND));
        if (!alert.getGroupTrip().getMatchingGroup().getMatchingGroupId().equals(groupId)) {
            throw new AppException(ErrorCode.SOS_ALERT_NOT_FOUND);
        }
        return alert;
    }

    private User requireSenderOrLeader(UUID groupId, SosAlert alert, UUID userId) {
        if (alert.getSender().getUserId().equals(userId)) {
            return alert.getSender();
        }
        MatchingMember member = matchingMemberRepository.findByGroupIdAndUserId(groupId, userId)
                .filter(m -> m.getStatus() == JoinStatus.ACCEPTED)
                .filter(m -> m.getRole() == MatchingRole.LEADER)
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHORIZED_RESOLVE_SOS));
        return member.getUser();
    }

    private void assertTransitionAllowed(SosAlertStatus current) {
        if (current != SosAlertStatus.OPEN) {
            throw new AppException(ErrorCode.SOS_ALERT_ALREADY_RESOLVED);
        }
    }

    private void broadcastAfterCommit(UUID groupId, SosAlertResponse response) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                messagingTemplate.convertAndSend("/topic/matching-groups/" + groupId + "/sos", response);
            }
        });
    }
}
