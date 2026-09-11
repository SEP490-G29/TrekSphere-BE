package com.sep.treksphere.matching.service.impl;

import com.sep.treksphere.common.exception.AppException;
import com.sep.treksphere.common.exception.ErrorCode;
import com.sep.treksphere.matching.dto.request.CustomJourneyActivityCreateRequest;
import com.sep.treksphere.matching.dto.request.CustomJourneyActivityUpdateRequest;
import com.sep.treksphere.matching.dto.request.CustomJourneyCheckpointCreateRequest;
import com.sep.treksphere.matching.dto.request.CustomJourneyCheckpointUpdateRequest;
import com.sep.treksphere.matching.dto.request.CustomJourneyUpdateRequest;
import com.sep.treksphere.matching.dto.response.CustomJourneyActivityResponse;
import com.sep.treksphere.matching.dto.response.CustomJourneyCheckpointResponse;
import com.sep.treksphere.matching.dto.response.CustomJourneyDetailResponse;
import com.sep.treksphere.matching.entity.CustomJourney;
import com.sep.treksphere.matching.entity.CustomJourneyActivity;
import com.sep.treksphere.matching.entity.CustomJourneyCheckpoint;
import com.sep.treksphere.matching.entity.MatchingGroup;
import com.sep.treksphere.matching.entity.MatchingMember;
import com.sep.treksphere.matching.enums.JoinStatus;
import com.sep.treksphere.matching.enums.MatchingGroupStatus;
import com.sep.treksphere.matching.enums.MatchingRole;
import com.sep.treksphere.matching.mapper.CustomJourneyMapper;
import com.sep.treksphere.matching.repository.CustomJourneyActivityRepository;
import com.sep.treksphere.matching.repository.CustomJourneyCheckpointRepository;
import com.sep.treksphere.matching.repository.CustomJourneyRepository;
import com.sep.treksphere.matching.repository.MatchingGroupRepository;
import com.sep.treksphere.matching.repository.MatchingMemberRepository;
import com.sep.treksphere.matching.service.CustomJourneyService;
import com.sep.treksphere.matching.dto.request.CustomJourneyCostItemCreateRequest;
import com.sep.treksphere.matching.dto.request.CustomJourneyCostItemUpdateRequest;
import com.sep.treksphere.matching.dto.response.CustomJourneyCostItemResponse;
import com.sep.treksphere.matching.dto.response.CustomJourneyCostSummaryResponse;
import com.sep.treksphere.matching.entity.CustomJourneyCostItem;
import com.sep.treksphere.matching.repository.CustomJourneyCostItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomJourneyServiceImpl implements CustomJourneyService {

    private final CustomJourneyRepository customJourneyRepository;
    private final CustomJourneyCheckpointRepository checkpointRepository;
    private final CustomJourneyActivityRepository activityRepository;
    private final CustomJourneyCostItemRepository costItemRepository;
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

        validateDayNoWithinJourney(journey, request.getDayNo());

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
        if (request.getDayNo() != null) {
            validateDayNoWithinJourney(journey, request.getDayNo());
        }

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
        checkpoint.setDeletedAt(java.time.LocalDateTime.now());
        checkpointRepository.save(checkpoint);
        log.info("Deleted checkpoint {} in custom journey {}", checkpointId, journey.getCustomJourneyId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomJourneyActivityResponse> getActivities(UUID groupId, UUID currentUserId) {
        MatchingGroup group = getGroupOrThrow(groupId);
        validateReadPermission(group, currentUserId);

        CustomJourney journey = customJourneyRepository.findByMatchingGroup_MatchingGroupIdAndIsDeletedFalse(groupId)
                .orElseThrow(() -> new AppException(ErrorCode.JOURNEY_NOT_FOUND));

        List<CustomJourneyActivity> activities = activityRepository
                .findByCustomJourney_CustomJourneyIdAndIsDeletedFalseOrderByDayNoAscTimeSlotAscActivityOrderAsc(journey.getCustomJourneyId());

        return activities.stream()
                .map(customJourneyMapper::toActivityResponse)
                .toList();
    }

    @Override
    @Transactional
    public CustomJourneyActivityResponse createActivity(
            UUID groupId, CustomJourneyActivityCreateRequest request, UUID currentUserId) {
        MatchingGroup group = getGroupOrThrow(groupId);
        validateLeaderPermission(group, currentUserId);

        CustomJourney journey = customJourneyRepository.findByMatchingGroup_MatchingGroupIdAndIsDeletedFalse(groupId)
                .orElseThrow(() -> new AppException(ErrorCode.JOURNEY_NOT_FOUND));

        validateJourneyNotLocked(journey);
        validateDayNoWithinJourney(journey, request.getDayNo());

        if (activityRepository.existsByCustomJourney_CustomJourneyIdAndDayNoAndTimeSlotAndActivityOrderAndIsDeletedFalse(
                journey.getCustomJourneyId(), request.getDayNo(), request.getTimeSlot(), request.getActivityOrder())) {
            throw new AppException(ErrorCode.ACTIVITY_ORDER_DUPLICATED);
        }

        validateTimeOrder(request.getPlannedStartAt(), request.getPlannedEndAt());

        CustomJourneyActivity activity = customJourneyMapper.toActivityEntity(request);
        activity.setCustomJourney(journey);

        if (request.getCustomJourneyCheckpointId() != null) {
            CustomJourneyCheckpoint checkpoint = checkpointRepository
                    .findByCustomJourneyCheckpointIdAndCustomJourney_CustomJourneyIdAndIsDeletedFalse(
                            request.getCustomJourneyCheckpointId(), journey.getCustomJourneyId())
                    .orElseThrow(() -> new AppException(ErrorCode.CUSTOM_JOURNEY_CHECKPOINT_NOT_FOUND));
            activity.setCheckpoint(checkpoint);
        }

        CustomJourneyActivity saved = activityRepository.save(activity);
        log.info("Created activity {} for custom journey {}", saved.getCustomJourneyActivityId(), journey.getCustomJourneyId());

        return customJourneyMapper.toActivityResponse(saved);
    }

    @Override
    @Transactional
    public CustomJourneyActivityResponse updateActivity(
            UUID groupId, UUID activityId, CustomJourneyActivityUpdateRequest request, UUID currentUserId) {
        MatchingGroup group = getGroupOrThrow(groupId);
        validateLeaderPermission(group, currentUserId);

        CustomJourney journey = customJourneyRepository.findByMatchingGroup_MatchingGroupIdAndIsDeletedFalse(groupId)
                .orElseThrow(() -> new AppException(ErrorCode.JOURNEY_NOT_FOUND));

        validateJourneyNotLocked(journey);
        if (request.getDayNo() != null) {
            validateDayNoWithinJourney(journey, request.getDayNo());
        }

        CustomJourneyActivity activity = activityRepository
                .findByCustomJourneyActivityIdAndCustomJourney_CustomJourneyIdAndIsDeletedFalse(activityId, journey.getCustomJourneyId())
                .orElseThrow(() -> new AppException(ErrorCode.CUSTOM_JOURNEY_ACTIVITY_NOT_FOUND));

        if (activityRepository.existsByCustomJourney_CustomJourneyIdAndDayNoAndTimeSlotAndActivityOrderAndCustomJourneyActivityIdNotAndIsDeletedFalse(
                journey.getCustomJourneyId(), request.getDayNo(), request.getTimeSlot(), request.getActivityOrder(), activityId)) {
            throw new AppException(ErrorCode.ACTIVITY_ORDER_DUPLICATED);
        }

        var plannedStart = request.getPlannedStartAt() != null ? request.getPlannedStartAt() : activity.getPlannedStartAt();
        var plannedEnd = request.getPlannedEndAt() != null ? request.getPlannedEndAt() : activity.getPlannedEndAt();
        validateTimeOrder(plannedStart, plannedEnd);

        customJourneyMapper.updateActivityFromRequest(request, activity);

        if (request.getCustomJourneyCheckpointId() != null) {
            CustomJourneyCheckpoint checkpoint = checkpointRepository
                    .findByCustomJourneyCheckpointIdAndCustomJourney_CustomJourneyIdAndIsDeletedFalse(
                            request.getCustomJourneyCheckpointId(), journey.getCustomJourneyId())
                    .orElseThrow(() -> new AppException(ErrorCode.CUSTOM_JOURNEY_CHECKPOINT_NOT_FOUND));
            activity.setCheckpoint(checkpoint);
        } else {
            activity.setCheckpoint(null);
        }

        CustomJourneyActivity saved = activityRepository.save(activity);
        log.info("Updated activity {} in custom journey {}", activityId, journey.getCustomJourneyId());

        return customJourneyMapper.toActivityResponse(saved);
    }

    @Override
    @Transactional
    public void deleteActivity(UUID groupId, UUID activityId, UUID currentUserId) {
        MatchingGroup group = getGroupOrThrow(groupId);
        validateLeaderPermission(group, currentUserId);

        CustomJourney journey = customJourneyRepository.findByMatchingGroup_MatchingGroupIdAndIsDeletedFalse(groupId)
                .orElseThrow(() -> new AppException(ErrorCode.JOURNEY_NOT_FOUND));

        validateJourneyNotLocked(journey);

        CustomJourneyActivity activity = activityRepository
                .findByCustomJourneyActivityIdAndCustomJourney_CustomJourneyIdAndIsDeletedFalse(activityId, journey.getCustomJourneyId())
                .orElseThrow(() -> new AppException(ErrorCode.CUSTOM_JOURNEY_ACTIVITY_NOT_FOUND));

        activity.setIsDeleted(true);
        activity.setDeletedAt(java.time.LocalDateTime.now());
        activityRepository.save(activity);
        log.info("Deleted activity {} in custom journey {}", activityId, journey.getCustomJourneyId());
    }

    @Override
    @Transactional(readOnly = true)
    public CustomJourneyCostSummaryResponse getCostSummary(UUID groupId, UUID currentUserId) {
        MatchingGroup group = getGroupOrThrow(groupId);
        validateReadPermission(group, currentUserId);

        CustomJourney journey = customJourneyRepository.findByMatchingGroup_MatchingGroupIdAndIsDeletedFalse(groupId)
                .orElseThrow(() -> new AppException(ErrorCode.JOURNEY_NOT_FOUND));

        List<CustomJourneyCostItem> costItems = costItemRepository
                .findByCustomJourney_CustomJourneyIdAndIsDeletedFalse(journey.getCustomJourneyId());

        List<CustomJourneyCostItemResponse> itemResponses = costItems.stream()
                .map(customJourneyMapper::toCostItemResponse)
                .toList();

        BigDecimal totalEstimatedCost = costItems.stream()
                .map(CustomJourneyCostItem::getEstimatedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int activeMemberCount = matchingMemberRepository.findActiveMembers(groupId, JoinStatus.ACCEPTED).size();
        int maxSize = group.getMaxSize() != null ? group.getMaxSize() : 1;
        int divisor = activeMemberCount > 0 ? activeMemberCount : (group.getCurrentSize() != null && group.getCurrentSize() > 0 ? group.getCurrentSize() : maxSize);
        if (divisor <= 0) divisor = 1;

        BigDecimal estimatedCostPerMember = totalEstimatedCost.divide(BigDecimal.valueOf(divisor), 2, RoundingMode.HALF_UP);

        return CustomJourneyCostSummaryResponse.builder()
                .customJourneyId(journey.getCustomJourneyId())
                .matchingGroupId(groupId)
                .totalEstimatedCost(totalEstimatedCost)
                .estimatedCostPerMember(estimatedCostPerMember)
                .activeMemberCount(activeMemberCount)
                .maxSize(group.getMaxSize())
                .costItems(itemResponses)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomJourneyCostItemResponse> getCostItems(UUID groupId, UUID currentUserId) {
        MatchingGroup group = getGroupOrThrow(groupId);
        validateReadPermission(group, currentUserId);

        CustomJourney journey = customJourneyRepository.findByMatchingGroup_MatchingGroupIdAndIsDeletedFalse(groupId)
                .orElseThrow(() -> new AppException(ErrorCode.JOURNEY_NOT_FOUND));

        List<CustomJourneyCostItem> costItems = costItemRepository
                .findByCustomJourney_CustomJourneyIdAndIsDeletedFalse(journey.getCustomJourneyId());

        return costItems.stream()
                .map(customJourneyMapper::toCostItemResponse)
                .toList();
    }

    @Override
    @Transactional
    public CustomJourneyCostItemResponse createCostItem(
            UUID groupId, CustomJourneyCostItemCreateRequest request, UUID currentUserId) {
        MatchingGroup group = getGroupOrThrow(groupId);
        validateLeaderPermission(group, currentUserId);

        CustomJourney journey = customJourneyRepository.findByMatchingGroup_MatchingGroupIdAndIsDeletedFalse(groupId)
                .orElseThrow(() -> new AppException(ErrorCode.JOURNEY_NOT_FOUND));

        validateJourneyNotLocked(journey);

        CustomJourneyCostItem costItem = customJourneyMapper.toCostItemEntity(request);
        costItem.setCustomJourney(journey);
        if (request.getItemName() != null) {
            costItem.setItemName(request.getItemName().trim());
        }

        CustomJourneyCostItem saved = costItemRepository.save(costItem);
        log.info("Created cost item {} for custom journey {}", saved.getCustomJourneyCostItemId(), journey.getCustomJourneyId());

        return customJourneyMapper.toCostItemResponse(saved);
    }

    @Override
    @Transactional
    public CustomJourneyCostItemResponse updateCostItem(
            UUID groupId, UUID costItemId, CustomJourneyCostItemUpdateRequest request, UUID currentUserId) {
        MatchingGroup group = getGroupOrThrow(groupId);
        validateLeaderPermission(group, currentUserId);

        CustomJourney journey = customJourneyRepository.findByMatchingGroup_MatchingGroupIdAndIsDeletedFalse(groupId)
                .orElseThrow(() -> new AppException(ErrorCode.JOURNEY_NOT_FOUND));

        validateJourneyNotLocked(journey);

        CustomJourneyCostItem costItem = costItemRepository
                .findByCustomJourneyCostItemIdAndCustomJourney_CustomJourneyIdAndIsDeletedFalse(costItemId, journey.getCustomJourneyId())
                .orElseThrow(() -> new AppException(ErrorCode.CUSTOM_JOURNEY_COST_ITEM_NOT_FOUND));

        customJourneyMapper.updateCostItemFromRequest(request, costItem);
        if (request.getItemName() != null && !request.getItemName().isBlank()) {
            costItem.setItemName(request.getItemName().trim());
        }
        if (request.getCategory() != null) {
            costItem.setCategory(request.getCategory());
        }
        if (request.getEstimatedAmount() != null) {
            costItem.setEstimatedAmount(request.getEstimatedAmount());
        }
        if (request.getNote() != null) {
            costItem.setNote(request.getNote());
        }

        CustomJourneyCostItem saved = costItemRepository.save(costItem);
        log.info("Updated cost item {} for custom journey {}", costItemId, journey.getCustomJourneyId());

        return customJourneyMapper.toCostItemResponse(saved);
    }

    @Override
    @Transactional
    public void deleteCostItem(UUID groupId, UUID costItemId, UUID currentUserId) {
        MatchingGroup group = getGroupOrThrow(groupId);
        validateLeaderPermission(group, currentUserId);

        CustomJourney journey = customJourneyRepository.findByMatchingGroup_MatchingGroupIdAndIsDeletedFalse(groupId)
                .orElseThrow(() -> new AppException(ErrorCode.JOURNEY_NOT_FOUND));

        validateJourneyNotLocked(journey);

        CustomJourneyCostItem costItem = costItemRepository
                .findByCustomJourneyCostItemIdAndCustomJourney_CustomJourneyIdAndIsDeletedFalse(costItemId, journey.getCustomJourneyId())
                .orElseThrow(() -> new AppException(ErrorCode.CUSTOM_JOURNEY_COST_ITEM_NOT_FOUND));

        costItem.setIsDeleted(true);
        costItem.setDeletedAt(java.time.LocalDateTime.now());
        costItemRepository.save(costItem);
        log.info("Deleted cost item {} for custom journey {}", costItemId, journey.getCustomJourneyId());
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

    private void validateTimeOrder(String start, String end) {
        if (start != null && !start.isBlank() && end != null && !end.isBlank()) {
            if (end.trim().compareTo(start.trim()) < 0) {
                throw new AppException(ErrorCode.CHECKPOINT_TIME_INVALID);
            }
        }
    }

    private void validateDayNoWithinJourney(CustomJourney journey, Integer dayNo) {
        if (dayNo != null) {
            if (dayNo < 1) {
                throw new AppException(ErrorCode.CUSTOM_JOURNEY_DATE_INVALID);
            }
            if (journey.getStartDate() != null && journey.getEndDate() != null) {
                long totalDays = ChronoUnit.DAYS.between(journey.getStartDate(), journey.getEndDate()) + 1;
                if (dayNo > totalDays) {
                    throw new AppException(ErrorCode.ACTIVITY_DAY_OUT_OF_RANGE);
                }
            }
        }
    }
}
