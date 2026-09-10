package com.sep.treksphere.matching.service.impl;

import com.sep.treksphere.common.exception.AppException;
import com.sep.treksphere.common.exception.ErrorCode;
import com.sep.treksphere.matching.dto.request.GroupChecklistFilterRequest;
import com.sep.treksphere.matching.dto.request.GroupChecklistItemCreateRequest;
import com.sep.treksphere.matching.dto.request.GroupChecklistItemStatusUpdateRequest;
import com.sep.treksphere.matching.dto.request.GroupChecklistItemUpdateRequest;
import com.sep.treksphere.matching.dto.response.GroupChecklistItemResponse;
import com.sep.treksphere.matching.dto.response.GroupChecklistSummaryResponse;
import com.sep.treksphere.matching.entity.GroupChecklistItem;
import com.sep.treksphere.matching.entity.MatchingGroup;
import com.sep.treksphere.matching.entity.MatchingMember;
import com.sep.treksphere.matching.enums.ChecklistItemScope;
import com.sep.treksphere.matching.enums.ChecklistItemStatus;
import com.sep.treksphere.matching.enums.JoinStatus;
import com.sep.treksphere.matching.enums.MatchingGroupStatus;
import com.sep.treksphere.matching.enums.MatchingRole;
import com.sep.treksphere.matching.mapper.GroupChecklistMapper;
import com.sep.treksphere.matching.repository.GroupChecklistItemRepository;
import com.sep.treksphere.matching.repository.MatchingGroupRepository;
import com.sep.treksphere.matching.repository.MatchingMemberRepository;
import com.sep.treksphere.matching.service.GroupChecklistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class GroupChecklistServiceImpl implements GroupChecklistService {

    private final GroupChecklistItemRepository checklistItemRepository;
    private final MatchingGroupRepository matchingGroupRepository;
    private final MatchingMemberRepository matchingMemberRepository;
    private final GroupChecklistMapper checklistMapper;

    @Override
    @Transactional(readOnly = true)
    public GroupChecklistSummaryResponse getChecklistSummary(
            UUID groupId, GroupChecklistFilterRequest filter, UUID currentUserId) {
        MatchingGroup group = getGroupOrThrow(groupId);
        validateReadPermission(group, currentUserId);

        List<GroupChecklistItem> allItems = checklistItemRepository
                .findByMatchingGroup_MatchingGroupIdAndIsDeletedFalseOrderByCreatedAtAsc(groupId);

        int total = allItems.size();
        int completed = (int) allItems.stream().filter(i -> i.getStatus() == ChecklistItemStatus.DONE).count();
        int shared = (int) allItems.stream().filter(i -> i.getItemScope() == ChecklistItemScope.SHARED).count();
        int personal = (int) allItems.stream().filter(i -> i.getItemScope() == ChecklistItemScope.PERSONAL).count();

        List<GroupChecklistItem> filteredItems;
        if (filter != null && hasAnyFilter(filter)) {
            filteredItems = checklistItemRepository.findWithFilters(
                    groupId,
                    filter.getItemScope(),
                    filter.getStatus(),
                    filter.getAssigneeMatchingMemberId(),
                    filter.getIsRequired(),
                    filter.getKeyword()
            );
        } else {
            filteredItems = allItems;
        }

        List<GroupChecklistItemResponse> itemResponses = checklistMapper.toResponseList(filteredItems);

        return GroupChecklistSummaryResponse.builder()
                .totalItems(total)
                .completedItems(completed)
                .sharedItems(shared)
                .personalItems(personal)
                .items(itemResponses)
                .build();
    }

    @Override
    @Transactional
    public GroupChecklistItemResponse createChecklistItem(
            UUID groupId, GroupChecklistItemCreateRequest request, UUID currentUserId) {
        MatchingGroup group = getGroupOrThrow(groupId);
        MatchingMember callerMember = getCallerMemberOrThrow(groupId, currentUserId);

        GroupChecklistItem item = checklistMapper.toEntity(request);
        item.setMatchingGroup(group);
        item.setStatus(ChecklistItemStatus.TODO);
        if (item.getIsRequired() == null) {
            item.setIsRequired(false);
        }

        if (request.getAssigneeMatchingMemberId() != null) {
            MatchingMember assignee = validateAndGetAssignee(groupId, request.getAssigneeMatchingMemberId(), callerMember);
            item.setAssigneeMatchingMember(assignee);
        } else if (request.getItemScope() == ChecklistItemScope.PERSONAL) {
            item.setAssigneeMatchingMember(callerMember);
        }

        GroupChecklistItem saved = checklistItemRepository.save(item);
        log.info("Created checklist item {} for group {}", saved.getGroupChecklistItemId(), groupId);
        return checklistMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public GroupChecklistItemResponse updateChecklistItem(
            UUID groupId, UUID itemId, GroupChecklistItemUpdateRequest request, UUID currentUserId) {
        getGroupOrThrow(groupId);
        MatchingMember callerMember = getCallerMemberOrThrow(groupId, currentUserId);

        GroupChecklistItem item = checklistItemRepository
                .findByGroupChecklistItemIdAndMatchingGroup_MatchingGroupIdAndIsDeletedFalse(itemId, groupId)
                .orElseThrow(() -> new AppException(ErrorCode.CHECKLIST_ITEM_NOT_FOUND));

        validateItemModifyPermission(item, callerMember);

        checklistMapper.updateEntityFromRequest(request, item);

        if (request.getAssigneeMatchingMemberId() != null) {
            MatchingMember assignee = validateAndGetAssignee(groupId, request.getAssigneeMatchingMemberId(), callerMember);
            item.setAssigneeMatchingMember(assignee);
        }

        if (request.getStatus() != null) {
            applyStatusTransition(item, request.getStatus(), callerMember);
        }

        GroupChecklistItem saved = checklistItemRepository.save(item);
        log.info("Updated checklist item {} in group {}", itemId, groupId);
        return checklistMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public GroupChecklistItemResponse updateItemStatus(
            UUID groupId, UUID itemId, GroupChecklistItemStatusUpdateRequest request, UUID currentUserId) {
        getGroupOrThrow(groupId);
        MatchingMember callerMember = getCallerMemberOrThrow(groupId, currentUserId);

        GroupChecklistItem item = checklistItemRepository
                .findByGroupChecklistItemIdAndMatchingGroup_MatchingGroupIdAndIsDeletedFalse(itemId, groupId)
                .orElseThrow(() -> new AppException(ErrorCode.CHECKLIST_ITEM_NOT_FOUND));

        validateItemStatusUpdatePermission(item, callerMember);

        applyStatusTransition(item, request.getStatus(), callerMember);

        GroupChecklistItem saved = checklistItemRepository.save(item);
        log.info("Updated checklist item {} status to {} by user {}", itemId, request.getStatus(), currentUserId);
        return checklistMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteChecklistItem(UUID groupId, UUID itemId, UUID currentUserId) {
        getGroupOrThrow(groupId);
        MatchingMember callerMember = getCallerMemberOrThrow(groupId, currentUserId);

        GroupChecklistItem item = checklistItemRepository
                .findByGroupChecklistItemIdAndMatchingGroup_MatchingGroupIdAndIsDeletedFalse(itemId, groupId)
                .orElseThrow(() -> new AppException(ErrorCode.CHECKLIST_ITEM_NOT_FOUND));

        validateItemModifyPermission(item, callerMember);

        item.setIsDeleted(true);
        checklistItemRepository.save(item);
        log.info("Deleted checklist item {} in group {}", itemId, groupId);
    }

    private MatchingGroup getGroupOrThrow(UUID groupId) {
        return matchingGroupRepository.findById(groupId)
                .filter(g -> !Boolean.TRUE.equals(g.getIsDeleted()))
                .orElseThrow(() -> new AppException(ErrorCode.MATCHING_GROUP_NOT_FOUND));
    }

    private MatchingMember getCallerMemberOrThrow(UUID groupId, UUID currentUserId) {
        if (currentUserId == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED_WORKSPACE_ACCESS);
        }
        return matchingMemberRepository.findActiveMembers(groupId, JoinStatus.ACCEPTED)
                .stream()
                .filter(m -> m.getUser().getUserId().equals(currentUserId))
                .findFirst()
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHORIZED_WORKSPACE_ACCESS));
    }

    private MatchingMember validateAndGetAssignee(UUID groupId, UUID assigneeMatchingMemberId, MatchingMember caller) {
        MatchingMember assignee = matchingMemberRepository.findById(assigneeMatchingMemberId)
                .filter(m -> !Boolean.TRUE.equals(m.getIsDeleted()))
                .filter(m -> m.getStatus() == JoinStatus.ACCEPTED)
                .filter(m -> m.getMatchingGroup().getMatchingGroupId().equals(groupId))
                .orElseThrow(() -> new AppException(ErrorCode.ASSIGNEE_NOT_IN_GROUP));

        if (caller.getRole() != MatchingRole.LEADER && !Objects.equals(assignee.getMatchingMemberId(), caller.getMatchingMemberId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED_CHECKLIST_ACTION);
        }

        return assignee;
    }

    private void validateItemModifyPermission(GroupChecklistItem item, MatchingMember caller) {
        if (caller.getRole() == MatchingRole.LEADER) {
            return;
        }
        if (item.getItemScope() == ChecklistItemScope.PERSONAL) {
            if (item.getAssigneeMatchingMember() != null &&
                    Objects.equals(item.getAssigneeMatchingMember().getMatchingMemberId(), caller.getMatchingMemberId())) {
                return;
            }
        }
        throw new AppException(ErrorCode.UNAUTHORIZED_CHECKLIST_ACTION);
    }

    private void validateItemStatusUpdatePermission(GroupChecklistItem item, MatchingMember caller) {
        if (caller.getRole() == MatchingRole.LEADER) {
            return;
        }
        if (item.getItemScope() == ChecklistItemScope.SHARED) {
            return;
        }
        if (item.getItemScope() == ChecklistItemScope.PERSONAL) {
            if (item.getAssigneeMatchingMember() != null &&
                    Objects.equals(item.getAssigneeMatchingMember().getMatchingMemberId(), caller.getMatchingMemberId())) {
                return;
            }
        }
        throw new AppException(ErrorCode.UNAUTHORIZED_CHECKLIST_ACTION);
    }

    private void applyStatusTransition(GroupChecklistItem item, ChecklistItemStatus newStatus, MatchingMember caller) {
        item.setStatus(newStatus);
        if (newStatus == ChecklistItemStatus.DONE) {
            item.setCompletedAt(LocalDateTime.now());
            item.setCompletedBy(caller);
        } else {
            item.setCompletedAt(null);
            item.setCompletedBy(null);
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

    private boolean hasAnyFilter(GroupChecklistFilterRequest filter) {
        return filter.getItemScope() != null
                || filter.getStatus() != null
                || filter.getAssigneeMatchingMemberId() != null
                || filter.getIsRequired() != null
                || (filter.getKeyword() != null && !filter.getKeyword().isBlank());
    }
}
