package com.sep.treksphere.matching.service;

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
import com.sep.treksphere.matching.enums.ChecklistItemType;
import com.sep.treksphere.matching.enums.JoinStatus;
import com.sep.treksphere.matching.enums.MatchingGroupStatus;
import com.sep.treksphere.matching.enums.MatchingRole;
import com.sep.treksphere.matching.mapper.GroupChecklistMapper;
import com.sep.treksphere.matching.repository.GroupChecklistItemRepository;
import com.sep.treksphere.matching.repository.MatchingGroupRepository;
import com.sep.treksphere.matching.repository.MatchingMemberRepository;
import com.sep.treksphere.matching.service.impl.GroupChecklistServiceImpl;
import com.sep.treksphere.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GroupChecklistServiceTest {

    @Mock
    private GroupChecklistItemRepository checklistItemRepository;

    @Mock
    private MatchingGroupRepository matchingGroupRepository;

    @Mock
    private MatchingMemberRepository matchingMemberRepository;

    @Spy
    private GroupChecklistMapper checklistMapper = Mappers.getMapper(GroupChecklistMapper.class);

    @InjectMocks
    private GroupChecklistServiceImpl checklistService;

    private UUID groupId;
    private UUID leaderId;
    private UUID member1Id;
    private UUID member2Id;
    private UUID outsiderId;

    private MatchingGroup group;
    private MatchingMember leaderMember;
    private MatchingMember member1;
    private MatchingMember member2;

    private GroupChecklistItem sharedItem;
    private GroupChecklistItem personalItem1;

    @BeforeEach
    void setUp() {
        groupId = UUID.randomUUID();
        leaderId = UUID.randomUUID();
        member1Id = UUID.randomUUID();
        member2Id = UUID.randomUUID();
        outsiderId = UUID.randomUUID();

        User leaderUser = new User();
        leaderUser.setUserId(leaderId);
        leaderUser.setFullName("Leader User");

        User user1 = new User();
        user1.setUserId(member1Id);
        user1.setFullName("Member One");

        User user2 = new User();
        user2.setUserId(member2Id);
        user2.setFullName("Member Two");

        group = new MatchingGroup();
        group.setMatchingGroupId(groupId);
        group.setStatus(MatchingGroupStatus.OPEN);
        group.setIsDeleted(false);
        group.setOwner(leaderUser);

        leaderMember = new MatchingMember();
        leaderMember.setMatchingMemberId(UUID.randomUUID());
        leaderMember.setMatchingGroup(group);
        leaderMember.setUser(leaderUser);
        leaderMember.setRole(MatchingRole.LEADER);
        leaderMember.setStatus(JoinStatus.ACCEPTED);
        leaderMember.setIsDeleted(false);

        member1 = new MatchingMember();
        member1.setMatchingMemberId(UUID.randomUUID());
        member1.setMatchingGroup(group);
        member1.setUser(user1);
        member1.setRole(MatchingRole.MEMBER);
        member1.setStatus(JoinStatus.ACCEPTED);
        member1.setIsDeleted(false);

        member2 = new MatchingMember();
        member2.setMatchingMemberId(UUID.randomUUID());
        member2.setMatchingGroup(group);
        member2.setUser(user2);
        member2.setRole(MatchingRole.MEMBER);
        member2.setStatus(JoinStatus.ACCEPTED);
        member2.setIsDeleted(false);

        sharedItem = new GroupChecklistItem();
        sharedItem.setGroupChecklistItemId(UUID.randomUUID());
        sharedItem.setMatchingGroup(group);
        sharedItem.setTitle("Lều 4 người");
        sharedItem.setItemScope(ChecklistItemScope.SHARED);
        sharedItem.setItemTypeCode(ChecklistItemType.TENT);
        sharedItem.setIsRequired(true);
        sharedItem.setStatus(ChecklistItemStatus.TODO);
        sharedItem.setIsDeleted(false);

        personalItem1 = new GroupChecklistItem();
        personalItem1.setGroupChecklistItemId(UUID.randomUUID());
        personalItem1.setMatchingGroup(group);
        personalItem1.setTitle("Áo mưa cá nhân");
        personalItem1.setItemScope(ChecklistItemScope.PERSONAL);
        personalItem1.setItemTypeCode(ChecklistItemType.CLOTHING);
        personalItem1.setIsRequired(true);
        personalItem1.setAssigneeMatchingMember(member1);
        personalItem1.setStatus(ChecklistItemStatus.TODO);
        personalItem1.setIsDeleted(false);
    }

    @Test
    @DisplayName("getChecklistSummary - Thành công tính toán thống kê và trả về danh sách")
    void getChecklistSummary_Success() {
        personalItem1.setStatus(ChecklistItemStatus.DONE);
        when(matchingGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(checklistItemRepository.findByMatchingGroup_MatchingGroupIdAndIsDeletedFalseOrderByCreatedAtAsc(groupId))
                .thenReturn(List.of(sharedItem, personalItem1));

        GroupChecklistSummaryResponse response = checklistService.getChecklistSummary(groupId, null, outsiderId);

        assertThat(response).isNotNull();
        assertThat(response.getTotalItems()).isEqualTo(2);
        assertThat(response.getCompletedItems()).isEqualTo(1);
        assertThat(response.getSharedItems()).isEqualTo(1);
        assertThat(response.getPersonalItems()).isEqualTo(1);
        assertThat(response.getItems()).hasSize(2);
    }

    @Test
    @DisplayName("getChecklistSummary - Áp dụng filter khi có điều kiện lọc")
    void getChecklistSummary_WithFilters() {
        GroupChecklistFilterRequest filter = GroupChecklistFilterRequest.builder()
                .itemScope(ChecklistItemScope.SHARED)
                .build();

        when(matchingGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(checklistItemRepository.findByMatchingGroup_MatchingGroupIdAndIsDeletedFalseOrderByCreatedAtAsc(groupId))
                .thenReturn(List.of(sharedItem, personalItem1));
        when(checklistItemRepository.findWithFilters(eq(groupId), eq(ChecklistItemScope.SHARED), any(), any(), any(), any()))
                .thenReturn(List.of(sharedItem));

        GroupChecklistSummaryResponse response = checklistService.getChecklistSummary(groupId, filter, outsiderId);

        assertThat(response.getTotalItems()).isEqualTo(2);
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getTitle()).isEqualTo("Lều 4 người");
    }

    @Test
    @DisplayName("getChecklistSummary - Nhóm HIDDEN từ chối outsider")
    void getChecklistSummary_HiddenGroupForbiddenForOutsider() {
        group.setStatus(MatchingGroupStatus.HIDDEN);
        when(matchingGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(matchingMemberRepository.existsByMatchingGroup_MatchingGroupIdAndUser_UserIdAndStatusAndIsDeletedFalse(
                groupId, outsiderId, JoinStatus.ACCEPTED)).thenReturn(false);

        assertThatThrownBy(() -> checklistService.getChecklistSummary(groupId, null, outsiderId))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.UNAUTHORIZED_WORKSPACE_ACCESS);
    }

    @Test
    @DisplayName("createChecklistItem - Tạo mục PERSONAL mặc định gán cho chính người tạo")
    void createChecklistItem_PersonalDefaultsToCaller() {
        GroupChecklistItemCreateRequest request = GroupChecklistItemCreateRequest.builder()
                .title("Áo ấm")
                .itemScope(ChecklistItemScope.PERSONAL)
                .itemTypeCode(ChecklistItemType.CLOTHING)
                .build();

        when(matchingGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(matchingMemberRepository.findActiveMembers(groupId, JoinStatus.ACCEPTED)).thenReturn(List.of(member1));
        when(checklistItemRepository.save(any(GroupChecklistItem.class))).thenAnswer(inv -> {
            GroupChecklistItem item = inv.getArgument(0);
            item.setGroupChecklistItemId(UUID.randomUUID());
            return item;
        });

        GroupChecklistItemResponse response = checklistService.createChecklistItem(groupId, request, member1Id);

        assertThat(response).isNotNull();
        assertThat(response.getTitle()).isEqualTo("Áo ấm");
        assertThat(response.getItemScope()).isEqualTo(ChecklistItemScope.PERSONAL);
        assertThat(response.getAssigneeMatchingMemberId()).isEqualTo(member1.getMatchingMemberId());
    }

    @Test
    @DisplayName("createChecklistItem - Ném lỗi khi Member cố ý giao việc cho thành viên khác")
    void createChecklistItem_MemberCannotAssignToOtherMember() {
        GroupChecklistItemCreateRequest request = GroupChecklistItemCreateRequest.builder()
                .title("Lều cá nhân")
                .itemScope(ChecklistItemScope.PERSONAL)
                .assigneeMatchingMemberId(member2.getMatchingMemberId())
                .build();

        when(matchingGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(matchingMemberRepository.findActiveMembers(groupId, JoinStatus.ACCEPTED)).thenReturn(List.of(member1));
        when(matchingMemberRepository.findById(member2.getMatchingMemberId())).thenReturn(Optional.of(member2));

        assertThatThrownBy(() -> checklistService.createChecklistItem(groupId, request, member1Id))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.UNAUTHORIZED_CHECKLIST_ACTION);
    }

    @Test
    @DisplayName("createChecklistItem - Leader có quyền giao việc cho thành viên khác")
    void createChecklistItem_LeaderCanAssignToOtherMember() {
        GroupChecklistItemCreateRequest request = GroupChecklistItemCreateRequest.builder()
                .title("Bộ sơ cứu")
                .itemScope(ChecklistItemScope.SHARED)
                .assigneeMatchingMemberId(member1.getMatchingMemberId())
                .build();

        when(matchingGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(matchingMemberRepository.findActiveMembers(groupId, JoinStatus.ACCEPTED)).thenReturn(List.of(leaderMember));
        when(matchingMemberRepository.findById(member1.getMatchingMemberId())).thenReturn(Optional.of(member1));
        when(checklistItemRepository.save(any(GroupChecklistItem.class))).thenAnswer(inv -> inv.getArgument(0));

        GroupChecklistItemResponse response = checklistService.createChecklistItem(groupId, request, leaderId);

        assertThat(response).isNotNull();
        assertThat(response.getAssigneeMatchingMemberId()).isEqualTo(member1.getMatchingMemberId());
    }

    @Test
    @DisplayName("createChecklistItem - Ném lỗi khi Assignee không thuộc nhóm")
    void createChecklistItem_AssigneeNotInGroup() {
        UUID otherMemberId = UUID.randomUUID();
        GroupChecklistItemCreateRequest request = GroupChecklistItemCreateRequest.builder()
                .title("Dụng cụ y tế")
                .itemScope(ChecklistItemScope.SHARED)
                .assigneeMatchingMemberId(otherMemberId)
                .build();

        when(matchingGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(matchingMemberRepository.findActiveMembers(groupId, JoinStatus.ACCEPTED)).thenReturn(List.of(leaderMember));
        when(matchingMemberRepository.findById(otherMemberId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> checklistService.createChecklistItem(groupId, request, leaderId))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ASSIGNEE_NOT_IN_GROUP);
    }

    @Test
    @DisplayName("updateChecklistItem - Leader có quyền cập nhật bất kỳ mục nào")
    void updateChecklistItem_LeaderCanUpdateAnyItem() {
        UUID itemId = sharedItem.getGroupChecklistItemId();
        GroupChecklistItemUpdateRequest request = GroupChecklistItemUpdateRequest.builder()
                .title("Lều 6 người")
                .build();

        when(matchingGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(matchingMemberRepository.findActiveMembers(groupId, JoinStatus.ACCEPTED)).thenReturn(List.of(leaderMember));
        when(checklistItemRepository.findByGroupChecklistItemIdAndMatchingGroup_MatchingGroupIdAndIsDeletedFalse(itemId, groupId))
                .thenReturn(Optional.of(sharedItem));
        when(checklistItemRepository.save(any(GroupChecklistItem.class))).thenAnswer(inv -> inv.getArgument(0));

        GroupChecklistItemResponse response = checklistService.updateChecklistItem(groupId, itemId, request, leaderId);

        assertThat(response.getTitle()).isEqualTo("Lều 6 người");
        verify(checklistItemRepository).save(sharedItem);
    }

    @Test
    @DisplayName("updateChecklistItem - Member không được sửa chi tiết mục SHARED")
    void updateChecklistItem_MemberCannotUpdateSharedItem() {
        UUID itemId = sharedItem.getGroupChecklistItemId();
        GroupChecklistItemUpdateRequest request = GroupChecklistItemUpdateRequest.builder()
                .title("Lều đổi tên")
                .build();

        when(matchingGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(matchingMemberRepository.findActiveMembers(groupId, JoinStatus.ACCEPTED)).thenReturn(List.of(member1));
        when(checklistItemRepository.findByGroupChecklistItemIdAndMatchingGroup_MatchingGroupIdAndIsDeletedFalse(itemId, groupId))
                .thenReturn(Optional.of(sharedItem));

        assertThatThrownBy(() -> checklistService.updateChecklistItem(groupId, itemId, request, member1Id))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.UNAUTHORIZED_CHECKLIST_ACTION);
    }

    @Test
    @DisplayName("updateChecklistItem - Member không được sửa mục PERSONAL của người khác")
    void updateChecklistItem_MemberCannotUpdateOtherPersonalItem() {
        UUID itemId = personalItem1.getGroupChecklistItemId();
        GroupChecklistItemUpdateRequest request = GroupChecklistItemUpdateRequest.builder()
                .title("Đổi tên áo mưa")
                .build();

        when(matchingGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(matchingMemberRepository.findActiveMembers(groupId, JoinStatus.ACCEPTED)).thenReturn(List.of(member2));
        when(checklistItemRepository.findByGroupChecklistItemIdAndMatchingGroup_MatchingGroupIdAndIsDeletedFalse(itemId, groupId))
                .thenReturn(Optional.of(personalItem1));

        assertThatThrownBy(() -> checklistService.updateChecklistItem(groupId, itemId, request, member2Id))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.UNAUTHORIZED_CHECKLIST_ACTION);
    }

    @Test
    @DisplayName("updateItemStatus - Thành viên bất kỳ có thể đánh dấu DONE mục SHARED")
    void updateItemStatus_SharedItem_AnyMemberCanUpdateStatus() {
        UUID itemId = sharedItem.getGroupChecklistItemId();
        GroupChecklistItemStatusUpdateRequest request = GroupChecklistItemStatusUpdateRequest.builder()
                .status(ChecklistItemStatus.DONE)
                .build();

        when(matchingGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(matchingMemberRepository.findActiveMembers(groupId, JoinStatus.ACCEPTED)).thenReturn(List.of(member1));
        when(checklistItemRepository.findByGroupChecklistItemIdAndMatchingGroup_MatchingGroupIdAndIsDeletedFalse(itemId, groupId))
                .thenReturn(Optional.of(sharedItem));
        when(checklistItemRepository.save(any(GroupChecklistItem.class))).thenAnswer(inv -> inv.getArgument(0));

        GroupChecklistItemResponse response = checklistService.updateItemStatus(groupId, itemId, request, member1Id);

        assertThat(response.getStatus()).isEqualTo(ChecklistItemStatus.DONE);
        assertThat(sharedItem.getCompletedAt()).isNotNull();
        assertThat(sharedItem.getCompletedBy()).isEqualTo(member1);
    }

    @Test
    @DisplayName("updateItemStatus - Đổi trạng thái từ DONE về IN_PROGRESS xóa thông tin completedAt và completedBy")
    void updateItemStatus_ResetFromDone_ClearsCompletedAtAndBy() {
        sharedItem.setStatus(ChecklistItemStatus.DONE);
        sharedItem.setCompletedBy(member1);
        UUID itemId = sharedItem.getGroupChecklistItemId();

        GroupChecklistItemStatusUpdateRequest request = GroupChecklistItemStatusUpdateRequest.builder()
                .status(ChecklistItemStatus.IN_PROGRESS)
                .build();

        when(matchingGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(matchingMemberRepository.findActiveMembers(groupId, JoinStatus.ACCEPTED)).thenReturn(List.of(member1));
        when(checklistItemRepository.findByGroupChecklistItemIdAndMatchingGroup_MatchingGroupIdAndIsDeletedFalse(itemId, groupId))
                .thenReturn(Optional.of(sharedItem));
        when(checklistItemRepository.save(any(GroupChecklistItem.class))).thenAnswer(inv -> inv.getArgument(0));

        GroupChecklistItemResponse response = checklistService.updateItemStatus(groupId, itemId, request, member1Id);

        assertThat(response.getStatus()).isEqualTo(ChecklistItemStatus.IN_PROGRESS);
        assertThat(sharedItem.getCompletedAt()).isNull();
        assertThat(sharedItem.getCompletedBy()).isNull();
    }

    @Test
    @DisplayName("updateItemStatus - Người khác không được đổi status mục PERSONAL")
    void updateItemStatus_PersonalItem_OtherMemberForbidden() {
        UUID itemId = personalItem1.getGroupChecklistItemId();
        GroupChecklistItemStatusUpdateRequest request = GroupChecklistItemStatusUpdateRequest.builder()
                .status(ChecklistItemStatus.DONE)
                .build();

        when(matchingGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(matchingMemberRepository.findActiveMembers(groupId, JoinStatus.ACCEPTED)).thenReturn(List.of(member2));
        when(checklistItemRepository.findByGroupChecklistItemIdAndMatchingGroup_MatchingGroupIdAndIsDeletedFalse(itemId, groupId))
                .thenReturn(Optional.of(personalItem1));

        assertThatThrownBy(() -> checklistService.updateItemStatus(groupId, itemId, request, member2Id))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.UNAUTHORIZED_CHECKLIST_ACTION);
    }

    @Test
    @DisplayName("deleteChecklistItem - Leader xoá mềm thành công")
    void deleteChecklistItem_LeaderSuccess() {
        UUID itemId = sharedItem.getGroupChecklistItemId();

        when(matchingGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(matchingMemberRepository.findActiveMembers(groupId, JoinStatus.ACCEPTED)).thenReturn(List.of(leaderMember));
        when(checklistItemRepository.findByGroupChecklistItemIdAndMatchingGroup_MatchingGroupIdAndIsDeletedFalse(itemId, groupId))
                .thenReturn(Optional.of(sharedItem));

        checklistService.deleteChecklistItem(groupId, itemId, leaderId);

        assertThat(sharedItem.getIsDeleted()).isTrue();
        verify(checklistItemRepository).save(sharedItem);
    }
}
