package com.sep.treksphere.matching.service.impl;

import com.sep.treksphere.common.dto.BaseFilterRequest;
import com.sep.treksphere.common.dto.PaginationResponse;
import com.sep.treksphere.common.exception.AppException;
import com.sep.treksphere.common.exception.ErrorCode;
import com.sep.treksphere.common.util.PaginationUtils;
import com.sep.treksphere.matching.dto.request.MomentCreateRequest;
import com.sep.treksphere.matching.dto.request.MomentFilterRequest;
import com.sep.treksphere.matching.dto.request.MomentHideRequest;
import com.sep.treksphere.matching.dto.request.MomentUpdateRequest;
import com.sep.treksphere.matching.dto.request.MomentVisibilityUpdateRequest;
import com.sep.treksphere.matching.dto.response.MomentMapResponse;
import com.sep.treksphere.matching.dto.response.MomentMediaResponse;
import com.sep.treksphere.matching.dto.response.MomentResponse;
import com.sep.treksphere.matching.entity.MatchingGroup;
import com.sep.treksphere.matching.entity.MatchingMember;
import com.sep.treksphere.matching.entity.Moment;
import com.sep.treksphere.matching.entity.MomentMedia;
import com.sep.treksphere.matching.enums.JoinStatus;
import com.sep.treksphere.matching.enums.MatchingGroupStatus;
import com.sep.treksphere.matching.enums.MatchingRole;
import com.sep.treksphere.matching.enums.MomentStatus;
import com.sep.treksphere.matching.enums.MomentVisibility;
import com.sep.treksphere.matching.mapper.MomentMapper;
import com.sep.treksphere.matching.repository.MatchingGroupRepository;
import com.sep.treksphere.matching.repository.MatchingMemberRepository;
import com.sep.treksphere.matching.repository.MomentMediaRepository;
import com.sep.treksphere.matching.repository.MomentRepository;
import com.sep.treksphere.matching.service.MomentService;
import com.sep.treksphere.notification.NotificationEventType;
import com.sep.treksphere.notification.NotificationService;
import com.sep.treksphere.notification.ReferenceType;
import com.sep.treksphere.user.User;
import com.sep.treksphere.user.UserRepository;
import com.sep.treksphere.user.UserStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MomentServiceImpl implements MomentService {

    private final MomentRepository momentRepository;
    private final MomentMediaRepository momentMediaRepository;
    private final MatchingGroupRepository matchingGroupRepository;
    private final MatchingMemberRepository matchingMemberRepository;
    private final UserRepository userRepository;
    private final MomentMapper momentMapper;
    private final NotificationService notificationService;

    // ==========================================
    // GROUP MOMENTS
    // ==========================================

    @Override
    @Transactional
    public MomentResponse createGroupMoment(UUID groupId, UUID currentUserId, MomentCreateRequest request) {
        User user = getUserByIdOrThrow(currentUserId);
        MatchingGroup group = getGroupOrThrow(groupId);
        validateGroupStatusForMomentCreation(group);
        MatchingMember member = getAcceptedMemberOrThrow(groupId, currentUserId);

        validateMediaUrls(request.getMediaUrls());

        MomentVisibility visibility = request.getVisibility() != null
                ? request.getVisibility()
                : MomentVisibility.GROUP_ONLY;

        Moment moment = Moment.builder()
                .authorUser(user)
                .matchingGroup(group)
                .authorMatchingMember(member)
                .caption(request.getCaption())
                .capturedAt(request.getCapturedAt() != null ? request.getCapturedAt() : LocalDateTime.now())
                .placeName(request.getPlaceName())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .visibility(visibility)
                .status(MomentStatus.VISIBLE)
                .mediaList(new ArrayList<>())
                .build();

        attachMediaList(moment, request.getMediaUrls());

        Moment savedMoment = momentRepository.save(moment);
        log.info("Created group moment {} in group {} by user {}", savedMoment.getMomentId(), groupId, currentUserId);

        List<UUID> memberIdsToNotify = matchingMemberRepository.findActiveMembers(groupId, JoinStatus.ACCEPTED)
                .stream()
                .map(m -> m.getUser().getUserId())
                .filter(id -> !id.equals(currentUserId))
                .toList();

        notificationService.notify(
                memberIdsToNotify,
                NotificationEventType.GROUP_MOMENT_CREATED,
                ReferenceType.MATCHING_GROUP, groupId,
                "/trekker/my-groups/" + groupId + "?tab=moments",
                user.getFullName(), group.getGroupName());

        return momentMapper.toResponse(savedMoment);
    }

    @Override
    @Transactional
    public MomentResponse updateGroupMoment(UUID groupId, UUID momentId, UUID currentUserId, MomentUpdateRequest request) {
        getUserByIdOrThrow(currentUserId);
        Moment moment = getMomentOrThrow(momentId);
        validateGroupMomentOwnership(groupId, moment, currentUserId);

        momentMapper.updateEntityFromRequest(request, moment);

        if (request.getMediaUrls() != null && !request.getMediaUrls().isEmpty()) {
            moment.getMediaList().clear();
            attachMediaList(moment, request.getMediaUrls());
        }

        Moment updatedMoment = momentRepository.save(moment);
        log.info("Updated group moment {} in group {} by user {}", momentId, groupId, currentUserId);
        return momentMapper.toResponse(updatedMoment);
    }

    @Override
    @Transactional
    public void deleteGroupMoment(UUID groupId, UUID momentId, UUID currentUserId) {
        getUserByIdOrThrow(currentUserId);
        Moment moment = getMomentOrThrow(momentId);
        validateGroupMomentOwnership(groupId, moment, currentUserId);

        moment.setIsDeleted(true);
        momentRepository.save(moment);
        log.info("Soft-deleted group moment {} in group {} by user {}", momentId, groupId, currentUserId);
    }

    @Override
    @Transactional
    public MomentResponse updateGroupMomentVisibility(
            UUID groupId, UUID momentId, UUID currentUserId, MomentVisibilityUpdateRequest request) {
        getUserByIdOrThrow(currentUserId);
        Moment moment = getMomentOrThrow(momentId);
        validateGroupMomentOwnership(groupId, moment, currentUserId);

        moment.setVisibility(request.getVisibility());
        Moment updatedMoment = momentRepository.save(moment);
        log.info("Updated visibility for moment {} to {} by user {}", momentId, request.getVisibility(), currentUserId);
        return momentMapper.toResponse(updatedMoment);
    }

    @Override
    @Transactional
    public MomentResponse hideGroupMoment(UUID groupId, UUID momentId, UUID currentUserId, MomentHideRequest request) {
        User user = getUserByIdOrThrow(currentUserId);
        Moment moment = getMomentOrThrow(momentId);
        validateGroupMomentContext(groupId, moment);
        validateLeaderOrAdmin(groupId, user);

        if (moment.getStatus() == MomentStatus.HIDDEN) {
            throw new AppException(ErrorCode.MOMENT_ALREADY_HIDDEN);
        }

        moment.setStatus(MomentStatus.HIDDEN);
        moment.setHiddenByUser(user);
        moment.setHiddenReason(request.getHiddenReason());

        Moment updatedMoment = momentRepository.save(moment);
        log.info("Moment {} in group {} hidden by leader/admin {}", momentId, groupId, currentUserId);
        return momentMapper.toResponse(updatedMoment);
    }

    @Override
    @Transactional
    public MomentResponse unhideGroupMoment(UUID groupId, UUID momentId, UUID currentUserId) {
        User user = getUserByIdOrThrow(currentUserId);
        Moment moment = getMomentOrThrow(momentId);
        validateGroupMomentContext(groupId, moment);
        validateLeaderOrAdmin(groupId, user);

        if (moment.getStatus() != MomentStatus.HIDDEN) {
            throw new AppException(ErrorCode.MOMENT_NOT_HIDDEN);
        }

        moment.setStatus(MomentStatus.VISIBLE);
        moment.setHiddenByUser(null);
        moment.setHiddenReason(null);

        Moment updatedMoment = momentRepository.save(moment);
        log.info("Moment {} in group {} unhidden by leader/admin {}", momentId, groupId, currentUserId);
        return momentMapper.toResponse(updatedMoment);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginationResponse<MomentResponse> getGroupMoments(UUID groupId, UUID currentUserId, MomentFilterRequest filter) {
        getUserByIdOrThrow(currentUserId);
        getGroupOrThrow(groupId);
        getAcceptedMemberOrThrow(groupId, currentUserId);

        boolean isLeader = isGroupLeader(groupId, currentUserId);
        Pageable pageable = filter != null ? filter.getPageable() : PageRequest.of(0, 10, Sort.by("createdAt").descending());

        Page<Moment> page;
        if (isLeader) {
            if (filter != null && filter.getStatus() != null) {
                page = momentRepository.findByMatchingGroup_MatchingGroupIdAndStatusAndIsDeletedFalse(
                        groupId, filter.getStatus(), pageable);
            } else {
                page = momentRepository.findByMatchingGroup_MatchingGroupIdAndIsDeletedFalse(groupId, pageable);
            }
        } else {
            page = momentRepository.findByMatchingGroup_MatchingGroupIdAndStatusAndIsDeletedFalse(
                    groupId, MomentStatus.VISIBLE, pageable);
        }

        Page<MomentResponse> responsePage = page.map(momentMapper::toResponse);
        return PaginationUtils.toPaginationResponse(responsePage);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginationResponse<MomentMediaResponse> getGroupAlbum(UUID groupId, UUID currentUserId, BaseFilterRequest filter) {
        getUserByIdOrThrow(currentUserId);
        getGroupOrThrow(groupId);
        getAcceptedMemberOrThrow(groupId, currentUserId);

        boolean isLeader = isGroupLeader(groupId, currentUserId);
        Pageable pageable = filter != null ? filter.getPageable() : PageRequest.of(0, 20, Sort.by("createdAt").descending());

        Page<MomentMedia> page;
        if (isLeader) {
            page = momentMediaRepository.findByMoment_MatchingGroup_MatchingGroupIdAndMoment_IsDeletedFalseOrderByCreatedAtDesc(
                    groupId, pageable);
        } else {
            page = momentMediaRepository.findByMoment_MatchingGroup_MatchingGroupIdAndMoment_StatusAndMoment_IsDeletedFalseOrderByCreatedAtDesc(
                    groupId, MomentStatus.VISIBLE, pageable);
        }

        Page<MomentMediaResponse> responsePage = page.map(momentMapper::toMediaResponse);
        return PaginationUtils.toPaginationResponse(responsePage);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MomentMapResponse> getGroupMomentsMap(UUID groupId, UUID currentUserId) {
        getUserByIdOrThrow(currentUserId);
        getGroupOrThrow(groupId);
        getAcceptedMemberOrThrow(groupId, currentUserId);

        boolean isLeader = isGroupLeader(groupId, currentUserId);

        List<Moment> list;
        if (isLeader) {
            list = momentRepository.findByMatchingGroup_MatchingGroupIdAndLatitudeIsNotNullAndLongitudeIsNotNullAndIsDeletedFalse(groupId);
        } else {
            list = momentRepository.findByMatchingGroup_MatchingGroupIdAndLatitudeIsNotNullAndLongitudeIsNotNullAndStatusAndIsDeletedFalse(
                    groupId, MomentStatus.VISIBLE);
        }

        return momentMapper.toMapResponseList(list);
    }

    @Override
    @Transactional(readOnly = true)
    public MomentResponse getGroupMomentDetail(UUID groupId, UUID momentId, UUID currentUserId) {
        getUserByIdOrThrow(currentUserId);
        Moment moment = getMomentOrThrow(momentId);
        validateGroupMomentContext(groupId, moment);
        getAcceptedMemberOrThrow(groupId, currentUserId);

        boolean isLeader = isGroupLeader(groupId, currentUserId);
        boolean isAuthor = moment.getAuthorUser().getUserId().equals(currentUserId);

        if (moment.getStatus() == MomentStatus.HIDDEN && !isLeader && !isAuthor) {
            throw new AppException(ErrorCode.MOMENT_NOT_FOUND);
        }

        return momentMapper.toResponse(moment);
    }

    // ==========================================
    // PERSONAL MOMENTS & SHOWCASE
    // ==========================================

    @Override
    @Transactional
    public MomentResponse createPersonalMoment(UUID currentUserId, MomentCreateRequest request) {
        User user = getUserByIdOrThrow(currentUserId);
        validateMediaUrls(request.getMediaUrls());

        MomentVisibility visibility = request.getVisibility() != null
                ? request.getVisibility()
                : MomentVisibility.PUBLIC_PROFILE;

        Moment moment = Moment.builder()
                .authorUser(user)
                .matchingGroup(null)
                .authorMatchingMember(null)
                .caption(request.getCaption())
                .capturedAt(request.getCapturedAt() != null ? request.getCapturedAt() : LocalDateTime.now())
                .placeName(request.getPlaceName())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .visibility(visibility)
                .status(MomentStatus.VISIBLE)
                .mediaList(new ArrayList<>())
                .build();

        attachMediaList(moment, request.getMediaUrls());

        Moment savedMoment = momentRepository.save(moment);
        log.info("Created personal moment {} by user {}", savedMoment.getMomentId(), currentUserId);
        return momentMapper.toResponse(savedMoment);
    }

    @Override
    @Transactional
    public MomentResponse updatePersonalMoment(UUID momentId, UUID currentUserId, MomentUpdateRequest request) {
        getUserByIdOrThrow(currentUserId);
        Moment moment = getMomentOrThrow(momentId);
        validateMomentOwnership(moment, currentUserId);

        momentMapper.updateEntityFromRequest(request, moment);

        if (request.getMediaUrls() != null && !request.getMediaUrls().isEmpty()) {
            moment.getMediaList().clear();
            attachMediaList(moment, request.getMediaUrls());
        }

        Moment updatedMoment = momentRepository.save(moment);
        log.info("Updated personal moment {} by user {}", momentId, currentUserId);
        return momentMapper.toResponse(updatedMoment);
    }

    @Override
    @Transactional
    public void deletePersonalMoment(UUID momentId, UUID currentUserId) {
        getUserByIdOrThrow(currentUserId);
        Moment moment = getMomentOrThrow(momentId);
        validateMomentOwnership(moment, currentUserId);

        moment.setIsDeleted(true);
        momentRepository.save(moment);
        log.info("Soft-deleted personal moment {} by user {}", momentId, currentUserId);
    }

    @Override
    @Transactional
    public MomentResponse updatePersonalMomentVisibility(
            UUID momentId, UUID currentUserId, MomentVisibilityUpdateRequest request) {
        getUserByIdOrThrow(currentUserId);
        Moment moment = getMomentOrThrow(momentId);
        validateMomentOwnership(moment, currentUserId);

        moment.setVisibility(request.getVisibility());
        Moment updatedMoment = momentRepository.save(moment);
        log.info("Updated personal moment {} visibility to {} by user {}", momentId, request.getVisibility(), currentUserId);
        return momentMapper.toResponse(updatedMoment);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginationResponse<MomentResponse> getMyMoments(UUID currentUserId, MomentFilterRequest filter) {
        getUserByIdOrThrow(currentUserId);
        Pageable pageable = filter != null ? filter.getPageable() : PageRequest.of(0, 10, Sort.by("createdAt").descending());
        Page<Moment> page = momentRepository.findByAuthorUser_UserIdAndIsDeletedFalse(currentUserId, pageable);
        Page<MomentResponse> responsePage = page.map(momentMapper::toResponse);
        return PaginationUtils.toPaginationResponse(responsePage);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MomentMapResponse> getMyMomentsMap(UUID currentUserId) {
        getUserByIdOrThrow(currentUserId);
        List<Moment> list = momentRepository.findByAuthorUser_UserIdAndLatitudeIsNotNullAndLongitudeIsNotNullAndIsDeletedFalse(
                currentUserId);
        return momentMapper.toMapResponseList(list);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginationResponse<MomentResponse> getUserPublicMoments(UUID targetUserId, MomentFilterRequest filter) {
        getUserByIdOrThrow(targetUserId);
        Pageable pageable = filter != null ? filter.getPageable() : PageRequest.of(0, 10, Sort.by("createdAt").descending());
        Page<Moment> page = momentRepository.findByAuthorUser_UserIdAndVisibilityAndStatusAndIsDeletedFalse(
                targetUserId, MomentVisibility.PUBLIC_PROFILE, MomentStatus.VISIBLE, pageable);
        Page<MomentResponse> responsePage = page.map(momentMapper::toResponse);
        return PaginationUtils.toPaginationResponse(responsePage);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MomentMapResponse> getUserPublicMomentsMap(UUID targetUserId) {
        getUserByIdOrThrow(targetUserId);
        List<Moment> list = momentRepository.findByAuthorUser_UserIdAndLatitudeIsNotNullAndLongitudeIsNotNullAndIsDeletedFalse(targetUserId)
                .stream()
                .filter(m -> m.getVisibility() == MomentVisibility.PUBLIC_PROFILE && m.getStatus() == MomentStatus.VISIBLE)
                .toList();
        return momentMapper.toMapResponseList(list);
    }

    @Override
    @Transactional(readOnly = true)
    public MomentResponse getMomentDetail(UUID momentId, UUID currentUserId) {
        User currentUser = currentUserId != null ? userRepository.findById(currentUserId).orElse(null) : null;
        Moment moment = getMomentOrThrow(momentId);

        boolean isAuthor = currentUser != null && moment.getAuthorUser().getUserId().equals(currentUser.getUserId());

        if (moment.getMatchingGroup() != null) {
            if (currentUser == null) {
                throw new AppException(ErrorCode.NOT_ACCEPTED_MATCHING_MEMBER);
            }
            boolean isMember = matchingMemberRepository.existsByMatchingGroup_MatchingGroupIdAndUser_UserIdAndStatusAndIsDeletedFalse(
                    moment.getMatchingGroup().getMatchingGroupId(), currentUser.getUserId(), JoinStatus.ACCEPTED);
            if (!isMember && !isAuthor) {
                throw new AppException(ErrorCode.NOT_ACCEPTED_MATCHING_MEMBER);
            }
            boolean isLeader = isGroupLeader(moment.getMatchingGroup().getMatchingGroupId(), currentUser.getUserId());
            if (moment.getStatus() == MomentStatus.HIDDEN && !isLeader && !isAuthor) {
                throw new AppException(ErrorCode.MOMENT_NOT_FOUND);
            }
        } else {
            if (moment.getVisibility() == MomentVisibility.ONLY_ME && !isAuthor) {
                throw new AppException(ErrorCode.MOMENT_NOT_FOUND);
            }
            if (moment.getStatus() == MomentStatus.HIDDEN && !isAuthor) {
                throw new AppException(ErrorCode.MOMENT_NOT_FOUND);
            }
        }

        return momentMapper.toResponse(moment);
    }

    // ==========================================
    // HELPER METHODS
    // ==========================================

    private User getUserByIdOrThrow(UUID userId) {
        User user = userRepository.findById(userId)
                .filter(u -> !Boolean.TRUE.equals(u.getIsDeleted()))
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (user.getStatus() == UserStatus.LOCKED) {
            throw new AppException(ErrorCode.ACCOUNT_LOCKED);
        }
        if (user.getStatus() == UserStatus.DEACTIVATED) {
            throw new AppException(ErrorCode.ACCOUNT_DEACTIVATED);
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AppException(ErrorCode.USER_NOT_ACTIVE);
        }

        return user;
    }

    private MatchingGroup getGroupOrThrow(UUID groupId) {
        return matchingGroupRepository.findById(groupId)
                .filter(g -> !Boolean.TRUE.equals(g.getIsDeleted()))
                .orElseThrow(() -> new AppException(ErrorCode.MATCHING_GROUP_NOT_FOUND));
    }

    private void validateGroupStatusForMomentCreation(MatchingGroup group) {
        if (group.getStatus() != MatchingGroupStatus.IN_PROGRESS && group.getStatus() != MatchingGroupStatus.COMPLETED) {
            throw new AppException(ErrorCode.GROUP_NOT_READY_FOR_MOMENT);
        }
    }

    private Moment getMomentOrThrow(UUID momentId) {
        return momentRepository.findByMomentIdAndIsDeletedFalse(momentId)
                .orElseThrow(() -> new AppException(ErrorCode.MOMENT_NOT_FOUND));
    }

    private MatchingMember getAcceptedMemberOrThrow(UUID groupId, UUID userId) {
        return matchingMemberRepository.findByGroupIdAndUserId(groupId, userId)
                .filter(m -> m.getStatus() == JoinStatus.ACCEPTED)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_ACCEPTED_MATCHING_MEMBER));
    }

    private boolean isGroupLeader(UUID groupId, UUID userId) {
        return matchingMemberRepository.findByGroupIdAndUserId(groupId, userId)
                .filter(m -> m.getStatus() == JoinStatus.ACCEPTED && m.getRole() == MatchingRole.LEADER)
                .isPresent();
    }

    private void validateLeaderOrAdmin(UUID groupId, User user) {
        boolean isLeader = isGroupLeader(groupId, user.getUserId());
        boolean isAdmin = user.getRoles() != null && user.getRoles().stream()
                .anyMatch(r -> "ADMIN".equalsIgnoreCase(r.getRoleName()) || "ROLE_ADMIN".equalsIgnoreCase(r.getRoleName()));
        if (!isLeader && !isAdmin) {
            throw new AppException(ErrorCode.UNAUTHORIZED_MOMENT_ACTION);
        }
    }

    private void validateGroupMomentOwnership(UUID groupId, Moment moment, UUID userId) {
        validateGroupMomentContext(groupId, moment);
        validateMomentOwnership(moment, userId);
    }

    private void validateGroupMomentContext(UUID groupId, Moment moment) {
        if (moment.getMatchingGroup() == null || !moment.getMatchingGroup().getMatchingGroupId().equals(groupId)) {
            throw new AppException(ErrorCode.CROSS_GROUP_ACTION_NOT_ALLOWED);
        }
    }

    private void validateMomentOwnership(Moment moment, UUID userId) {
        if (!moment.getAuthorUser().getUserId().equals(userId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED_MOMENT_ACTION);
        }
    }

    private void validateMediaUrls(List<String> mediaUrls) {
        if (mediaUrls == null || mediaUrls.isEmpty()) {
            throw new AppException(ErrorCode.MOMENT_MEDIA_REQUIRED);
        }
    }

    private void attachMediaList(Moment moment, List<String> mediaUrls) {
        for (int i = 0; i < mediaUrls.size(); i++) {
            MomentMedia media = MomentMedia.builder()
                    .imageUrl(mediaUrls.get(i))
                    .sortOrder(i)
                    .createdAt(LocalDateTime.now())
                    .build();
            moment.addMedia(media);
        }
    }
}
