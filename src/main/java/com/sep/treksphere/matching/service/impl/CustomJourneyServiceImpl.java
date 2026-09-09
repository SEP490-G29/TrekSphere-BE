package com.sep.treksphere.matching.service.impl;

import com.sep.treksphere.common.exception.AppException;
import com.sep.treksphere.common.exception.ErrorCode;
import com.sep.treksphere.matching.dto.request.CustomJourneyCheckpointCreateRequest;
import com.sep.treksphere.matching.dto.request.CustomJourneyCheckpointUpdateRequest;
import com.sep.treksphere.matching.dto.request.CustomJourneyUpdateRequest;
import com.sep.treksphere.matching.dto.response.CustomJourneyCheckpointResponse;
import com.sep.treksphere.matching.dto.response.CustomJourneyDetailResponse;
import com.sep.treksphere.matching.entity.CustomJourney;
import com.sep.treksphere.matching.entity.CustomJourneyCheckpoint;
import com.sep.treksphere.matching.entity.MatchingGroup;
import com.sep.treksphere.matching.entity.MatchingMember;
import com.sep.treksphere.matching.enums.JoinStatus;
import com.sep.treksphere.matching.enums.MatchingGroupStatus;
import com.sep.treksphere.matching.enums.MatchingRole;
import com.sep.treksphere.matching.mapper.CustomJourneyMapper;
import com.sep.treksphere.matching.repository.CustomJourneyCheckpointRepository;
import com.sep.treksphere.matching.repository.CustomJourneyRepository;
import com.sep.treksphere.matching.repository.MatchingGroupRepository;
import com.sep.treksphere.matching.repository.MatchingMemberRepository;
import com.sep.treksphere.matching.service.CustomJourneyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomJourneyServiceImpl implements CustomJourneyService {

    private final CustomJourneyRepository customJourneyRepository;
    private final CustomJourneyCheckpointRepository checkpointRepository;
    private final MatchingGroupRepository matchingGroupRepository;
    private final MatchingMemberRepository matchingMemberRepository;
    private final CustomJourneyMapper customJourneyMapper;

    @Override
    @Transactional(readOnly = true)
    public CustomJourneyDetailResponse getJourneyByGroupId(UUID groupId, UUID currentUserId) {
        MatchingGroup group = getGroupOrThrow(groupId);
        validateReadPermission(group, currentUserId);

        CustomJourney journey = customJourneyRepository.findDetailByGroupId(groupId)
                .orElseThrow(() -> new AppException(ErrorCode.JOURNEY_NOT_FOUND));

        return customJourneyMapper.toDetailResponse(journey);
    }

    @Override
    @Transactional
    public CustomJourneyDetailResponse updateJourney(UUID groupId, CustomJourneyUpdateRequest request, UUID currentUserId) {
        MatchingGroup group = getGroupOrThrow(groupId);
        validateLeaderPermission(group, currentUserId);

        CustomJourney journey = customJourneyRepository.findDetailByGroupId(groupId)
                .orElseThrow(() -> new AppException(ErrorCode.JOURNEY_NOT_FOUND));

        validateJourneyNotLocked(journey);

        LocalDate startDate = request.getStartDate() != null ? request.getStartDate() : journey.getStartDate();
        LocalDate endDate = request.getEndDate() != null ? request.getEndDate() : journey.getEndDate();

        if (startDate != null && group.getTargetDate() != null && !startDate.equals(group.getTargetDate())) {
            throw new AppException(ErrorCode.CUSTOM_JOURNEY_TARGET_DATE_MISMATCH);
        }

        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new AppException(ErrorCode.CUSTOM_JOURNEY_DATE_INVALID);
        }

        customJourneyMapper.updateEntityFromRequest(request, journey);
        journey.setStartDate(startDate);
        journey.setEndDate(endDate);
        CustomJourney saved = customJourneyRepository.save(journey);
        log.info("Updated custom journey for group {}", groupId);

        return customJourneyMapper.toDetailResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomJourneyCheckpointResponse> getCheckpoints(UUID groupId, UUID currentUserId) {
        MatchingGroup group = getGroupOrThrow(groupId);
        validateReadPermission(group, currentUserId);

        CustomJourney journey = customJourneyRepository.findByMatchingGroup_MatchingGroupIdAndIsDeletedFalse(groupId)
                .orElseThrow(() -> new AppException(ErrorCode.JOURNEY_NOT_FOUND));

        List<CustomJourneyCheckpoint> checkpoints = checkpointRepository
                .findByCustomJourney_CustomJourneyIdAndIsDeletedFalseOrderByDayNoAscCheckpointOrderAsc(journey.getCustomJourneyId());

        return checkpoints.stream()
                .map(customJourneyMapper::toCheckpointResponse)
                .toList();
    }

    @Override
    @Transactional
    public CustomJourneyCheckpointResponse createCheckpoint(
            UUID groupId, CustomJourneyCheckpointCreateRequest request, UUID currentUserId) {
        MatchingGroup group = getGroupOrThrow(groupId);
        validateLeaderPermission(group, currentUserId);

        CustomJourney journey = customJourneyRepository.findByMatchingGroup_MatchingGroupIdAndIsDeletedFalse(groupId)
                .orElseThrow(() -> new AppException(ErrorCode.JOURNEY_NOT_FOUND));

        validateJourneyNotLocked(journey);

        if (checkpointRepository.existsByCustomJourney_CustomJourneyIdAndCheckpointOrderAndIsDeletedFalse(
                journey.getCustomJourneyId(), request.getCheckpointOrder())) {
            throw new AppException(ErrorCode.CHECKPOINT_ORDER_DUPLICATED);
        }

        if (request.getPlannedStartAt() != null && request.getPlannedEndAt() != null
                && request.getPlannedEndAt().isBefore(request.getPlannedStartAt())) {
            throw new AppException(ErrorCode.CHECKPOINT_TIME_INVALID);
        }

        CustomJourneyCheckpoint checkpoint = customJourneyMapper.toCheckpointEntity(request);
        checkpoint.setCustomJourney(journey);

        CustomJourneyCheckpoint saved = checkpointRepository.save(checkpoint);
        log.info("Created checkpoint {} for custom journey {}", saved.getCustomJourneyCheckpointId(), journey.getCustomJourneyId());

        return customJourneyMapper.toCheckpointResponse(saved);
    }

    @Override
    @Transactional
    public CustomJourneyCheckpointResponse updateCheckpoint(
            UUID groupId, UUID checkpointId, CustomJourneyCheckpointUpdateRequest request, UUID currentUserId) {
        MatchingGroup group = getGroupOrThrow(groupId);
        validateLeaderPermission(group, currentUserId);

        CustomJourney journey = customJourneyRepository.findByMatchingGroup_MatchingGroupIdAndIsDeletedFalse(groupId)
                .orElseThrow(() -> new AppException(ErrorCode.JOURNEY_NOT_FOUND));

        validateJourneyNotLocked(journey);

        CustomJourneyCheckpoint checkpoint = checkpointRepository
                .findByCustomJourneyCheckpointIdAndCustomJourney_CustomJourneyIdAndIsDeletedFalse(checkpointId, journey.getCustomJourneyId())
                .orElseThrow(() -> new AppException(ErrorCode.CUSTOM_JOURNEY_CHECKPOINT_NOT_FOUND));

        if (request.getCheckpointOrder() != null && checkpointRepository
                .existsByCustomJourney_CustomJourneyIdAndCheckpointOrderAndCustomJourneyCheckpointIdNotAndIsDeletedFalse(
                        journey.getCustomJourneyId(), request.getCheckpointOrder(), checkpointId)) {
            throw new AppException(ErrorCode.CHECKPOINT_ORDER_DUPLICATED);
        }

        var plannedStart = request.getPlannedStartAt() != null ? request.getPlannedStartAt() : checkpoint.getPlannedStartAt();
        var plannedEnd = request.getPlannedEndAt() != null ? request.getPlannedEndAt() : checkpoint.getPlannedEndAt();
        if (plannedStart != null && plannedEnd != null && plannedEnd.isBefore(plannedStart)) {
            throw new AppException(ErrorCode.CHECKPOINT_TIME_INVALID);
        }

        customJourneyMapper.updateCheckpointFromRequest(request, checkpoint);
        CustomJourneyCheckpoint saved = checkpointRepository.save(checkpoint);
        log.info("Updated checkpoint {} in custom journey {}", checkpointId, journey.getCustomJourneyId());

        return customJourneyMapper.toCheckpointResponse(saved);
    }

    @Override
    @Transactional
    public void deleteCheckpoint(UUID groupId, UUID checkpointId, UUID currentUserId) {
        MatchingGroup group = getGroupOrThrow(groupId);
        validateLeaderPermission(group, currentUserId);

        CustomJourney journey = customJourneyRepository.findByMatchingGroup_MatchingGroupIdAndIsDeletedFalse(groupId)
                .orElseThrow(() -> new AppException(ErrorCode.JOURNEY_NOT_FOUND));

        validateJourneyNotLocked(journey);

        CustomJourneyCheckpoint checkpoint = checkpointRepository
                .findByCustomJourneyCheckpointIdAndCustomJourney_CustomJourneyIdAndIsDeletedFalse(checkpointId, journey.getCustomJourneyId())
                .orElseThrow(() -> new AppException(ErrorCode.CUSTOM_JOURNEY_CHECKPOINT_NOT_FOUND));

        checkpoint.setIsDeleted(true);
        checkpointRepository.save(checkpoint);
        log.info("Deleted checkpoint {} in custom journey {}", checkpointId, journey.getCustomJourneyId());
    }

    private MatchingGroup getGroupOrThrow(UUID groupId) {
        return matchingGroupRepository.findById(groupId)
                .filter(g -> !Boolean.TRUE.equals(g.getIsDeleted()))
                .orElseThrow(() -> new AppException(ErrorCode.MATCHING_GROUP_NOT_FOUND));
    }

    private void validateJourneyNotLocked(CustomJourney journey) {
        if (Boolean.TRUE.equals(journey.getIsLocked())) {
            throw new AppException(ErrorCode.JOURNEY_LOCKED);
        }
    }

    private void validateLeaderPermission(MatchingGroup group, UUID currentUserId) {
        if (currentUserId == null) {
            throw new AppException(ErrorCode.MATCHING_GROUP_UNAUTHORIZED_MANAGE);
        }
        boolean isLeader = matchingMemberRepository.findActiveMembers(group.getMatchingGroupId(), JoinStatus.ACCEPTED)
                .stream()
                .anyMatch(m -> m.getUser().getUserId().equals(currentUserId) && m.getRole() == MatchingRole.LEADER);

        if (!isLeader) {
            throw new AppException(ErrorCode.MATCHING_GROUP_UNAUTHORIZED_MANAGE);
        }
    }

    private void validateReadPermission(MatchingGroup group, UUID currentUserId) {
        if (group.getStatus() == MatchingGroupStatus.OPEN || group.getStatus() == MatchingGroupStatus.FULL) {
            return;
        }
        if (currentUserId == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED_WORKSPACE_ACCESS);
        }
        boolean isAcceptedMember = matchingMemberRepository.existsByMatchingGroup_MatchingGroupIdAndUser_UserIdAndStatusAndIsDeletedFalse(
                group.getMatchingGroupId(), currentUserId, JoinStatus.ACCEPTED);
        if (!isAcceptedMember) {
            throw new AppException(ErrorCode.UNAUTHORIZED_WORKSPACE_ACCESS);
        }
    }
}
