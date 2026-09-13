package com.sep.treksphere.matching.service;

import com.sep.treksphere.common.exception.AppException;
import com.sep.treksphere.common.exception.ErrorCode;
import com.sep.treksphere.matching.dto.request.GroupPostCommentCreateRequest;
import com.sep.treksphere.matching.dto.request.GroupPostCommentUpdateRequest;
import com.sep.treksphere.matching.dto.request.GroupPostCreateRequest;
import com.sep.treksphere.matching.dto.request.GroupPostUpdateRequest;
import com.sep.treksphere.matching.dto.response.GroupPostCommentResponse;
import com.sep.treksphere.matching.dto.response.GroupPostDetailResponse;
import com.sep.treksphere.matching.dto.response.GroupPostResponse;
import com.sep.treksphere.matching.entity.GroupPost;
import com.sep.treksphere.matching.entity.GroupPostComment;
import com.sep.treksphere.matching.entity.MatchingGroup;
import com.sep.treksphere.matching.entity.MatchingMember;
import com.sep.treksphere.matching.enums.GroupContentStatus;
import com.sep.treksphere.matching.enums.JoinStatus;
import com.sep.treksphere.matching.enums.MatchingGroupStatus;
import com.sep.treksphere.matching.enums.MatchingRole;
import com.sep.treksphere.matching.mapper.GroupPostMapper;
import com.sep.treksphere.matching.repository.GroupPostCommentRepository;
import com.sep.treksphere.matching.repository.GroupPostRepository;
import com.sep.treksphere.matching.repository.MatchingGroupRepository;
import com.sep.treksphere.matching.repository.MatchingMemberRepository;
import com.sep.treksphere.matching.service.impl.GroupPostServiceImpl;
import com.sep.treksphere.notification.NotificationService;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class GroupPostServiceTest {

    @Mock
    private GroupPostRepository postRepository;

    @Mock
    private GroupPostCommentRepository commentRepository;

    @Mock
    private MatchingGroupRepository matchingGroupRepository;

    @Mock
    private MatchingMemberRepository matchingMemberRepository;

    @Mock
    private NotificationService notificationService;

    @Spy
    private GroupPostMapper postMapper = Mappers.getMapper(GroupPostMapper.class);

    @InjectMocks
    private GroupPostServiceImpl postService;

    private UUID groupId;
    private UUID leaderId;
    private UUID authorId;
    private UUID otherMemberId;
    private UUID outsiderId;

    private MatchingGroup group;
    private MatchingMember leaderMember;
    private MatchingMember authorMember;
    private MatchingMember otherMember;

    private GroupPost post;
    private GroupPostComment comment;

    @BeforeEach
    void setUp() {
        groupId = UUID.randomUUID();
        leaderId = UUID.randomUUID();
        authorId = UUID.randomUUID();
        otherMemberId = UUID.randomUUID();
        outsiderId = UUID.randomUUID();

        User leaderUser = new User();
        leaderUser.setUserId(leaderId);
        leaderUser.setFullName("Leader User");

        User authorUser = new User();
        authorUser.setUserId(authorId);
        authorUser.setFullName("Author User");

        User otherUser = new User();
        otherUser.setUserId(otherMemberId);
        otherUser.setFullName("Other Member");

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

        authorMember = new MatchingMember();
        authorMember.setMatchingMemberId(UUID.randomUUID());
        authorMember.setMatchingGroup(group);
        authorMember.setUser(authorUser);
        authorMember.setRole(MatchingRole.MEMBER);
        authorMember.setStatus(JoinStatus.ACCEPTED);
        authorMember.setIsDeleted(false);

        otherMember = new MatchingMember();
        otherMember.setMatchingMemberId(UUID.randomUUID());
        otherMember.setMatchingGroup(group);
        otherMember.setUser(otherUser);
        otherMember.setRole(MatchingRole.MEMBER);
        otherMember.setStatus(JoinStatus.ACCEPTED);
        otherMember.setIsDeleted(false);

        post = new GroupPost();
        post.setGroupPostId(UUID.randomUUID());
        post.setMatchingGroup(group);
        post.setPostedBy(authorMember);
        post.setTitle("Kế hoạch chuẩn bị lều");
        post.setContent("Mọi người mang theo lều nhé");
        post.setStatus(GroupContentStatus.SHOW);
        post.setIsDeleted(false);

        comment = new GroupPostComment();
        comment.setGroupPostCommentId(UUID.randomUUID());
        comment.setGroupPost(post);
        comment.setAnsweredBy(otherMember);
        comment.setContent("Mình có lều 4 người");
        comment.setStatus(GroupContentStatus.SHOW);
        comment.setIsDeleted(false);
    }

    @Test
    @DisplayName("getGroupPosts - Leader xem được tất cả bài đăng kể cả HIDDEN")
    void getGroupPosts_SuccessForLeader() {
        Pageable pageable = PageRequest.of(0, 10);
        when(matchingGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(matchingMemberRepository.findActiveMembers(groupId, JoinStatus.ACCEPTED)).thenReturn(List.of(leaderMember));
        when(postRepository.findByMatchingGroup_MatchingGroupIdAndIsDeletedFalse(groupId, pageable))
                .thenReturn(new PageImpl<>(List.of(post)));
        when(commentRepository.countByGroupPost_GroupPostIdAndIsDeletedFalse(post.getGroupPostId())).thenReturn(1L);

        Page<GroupPostResponse> result = postService.getGroupPosts(groupId, pageable, leaderId);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Kế hoạch chuẩn bị lều");
        assertThat(result.getContent().get(0).getCommentCount()).isEqualTo(1L);
    }

    @Test
    @DisplayName("getGroupPosts - Member thường chỉ xem các bài đăng SHOW")
    void getGroupPosts_SuccessForMember() {
        Pageable pageable = PageRequest.of(0, 10);
        when(matchingGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(matchingMemberRepository.findActiveMembers(groupId, JoinStatus.ACCEPTED)).thenReturn(List.of(authorMember));
        when(postRepository.findByMatchingGroup_MatchingGroupIdAndStatusAndIsDeletedFalse(groupId, GroupContentStatus.SHOW, pageable))
                .thenReturn(new PageImpl<>(List.of(post)));
        when(commentRepository.countByGroupPost_GroupPostIdAndStatusAndIsDeletedFalse(post.getGroupPostId(), GroupContentStatus.SHOW)).thenReturn(1L);

        Page<GroupPostResponse> result = postService.getGroupPosts(groupId, pageable, authorId);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getCommentCount()).isEqualTo(1L);
    }

    @Test
    @DisplayName("getGroupPosts - Nhóm HIDDEN từ chối outsider")
    void getGroupPosts_HiddenGroupForbiddenForOutsider() {
        group.setStatus(MatchingGroupStatus.HIDDEN);
        Pageable pageable = PageRequest.of(0, 10);
        when(matchingGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(matchingMemberRepository.existsByMatchingGroup_MatchingGroupIdAndUser_UserIdAndStatusAndIsDeletedFalse(
                groupId, outsiderId, JoinStatus.ACCEPTED)).thenReturn(false);

        assertThatThrownBy(() -> postService.getGroupPosts(groupId, pageable, outsiderId))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.UNAUTHORIZED_WORKSPACE_ACCESS);
    }

    @Test
    @DisplayName("getGroupPostDetail - Thành công lấy chi tiết bài đăng và bình luận")
    void getGroupPostDetail_Success() {
        UUID postId = post.getGroupPostId();
        when(matchingGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(postRepository.findByGroupPostIdAndMatchingGroup_MatchingGroupIdAndIsDeletedFalse(postId, groupId))
                .thenReturn(Optional.of(post));
        when(matchingMemberRepository.findActiveMembers(groupId, JoinStatus.ACCEPTED)).thenReturn(List.of(authorMember));
        when(commentRepository.findByGroupPost_GroupPostIdAndParentCommentIsNullAndStatusAndIsDeletedFalseOrderByCreatedAtAsc(
                postId, GroupContentStatus.SHOW)).thenReturn(List.of(comment));
        when(commentRepository.findByParentComment_GroupPostCommentIdAndStatusAndIsDeletedFalseOrderByCreatedAtAsc(
                comment.getGroupPostCommentId(), GroupContentStatus.SHOW)).thenReturn(List.of());

        GroupPostDetailResponse detail = postService.getGroupPostDetail(groupId, postId, authorId);


        assertThat(detail).isNotNull();
        assertThat(detail.getPost().getTitle()).isEqualTo("Kế hoạch chuẩn bị lều");
        assertThat(detail.getComments()).hasSize(1);
        assertThat(detail.getComments().get(0).getContent()).isEqualTo("Mình có lều 4 người");
    }


    @Test
    @DisplayName("createGroupPost - Thành viên tạo bài đăng kèm ảnh thành công")
    void createGroupPost_Success() {
        GroupPostCreateRequest request = GroupPostCreateRequest.builder()
                .title("Thông báo mới")
                .content("Nội dung thông báo")
                .imageUrls(List.of("https://res.cloudinary.com/demo/image1.jpg", "https://res.cloudinary.com/demo/image2.jpg"))
                .build();

        when(matchingGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(matchingMemberRepository.findActiveMembers(groupId, JoinStatus.ACCEPTED)).thenReturn(List.of(authorMember));
        when(postRepository.save(any(GroupPost.class))).thenAnswer(inv -> {
            GroupPost p = inv.getArgument(0);
            p.setGroupPostId(UUID.randomUUID());
            return p;
        });

        GroupPostResponse response = postService.createGroupPost(groupId, request, authorId);

        assertThat(response).isNotNull();
        assertThat(response.getTitle()).isEqualTo("Thông báo mới");
        assertThat(response.getContent()).isEqualTo("Nội dung thông báo");
        assertThat(response.getImageUrls()).hasSize(2);
        assertThat(response.getStatus()).isEqualTo(GroupContentStatus.SHOW);
    }

    @Test
    @DisplayName("createGroupPost - Leader tạo bài đăng gửi thông báo GROUP_POST_ANNOUNCEMENT")
    void createGroupPost_LeaderSendsAnnouncementNotification() {
        GroupPostCreateRequest request = GroupPostCreateRequest.builder()
                .title("Thông báo họp nhóm khẩn")
                .content("Họp lúc 20h tối nay")
                .build();

        when(matchingGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(matchingMemberRepository.findActiveMembers(groupId, JoinStatus.ACCEPTED)).thenReturn(List.of(leaderMember, authorMember, otherMember));
        when(postRepository.save(any(GroupPost.class))).thenAnswer(inv -> {
            GroupPost p = inv.getArgument(0);
            p.setGroupPostId(UUID.randomUUID());
            return p;
        });

        GroupPostResponse response = postService.createGroupPost(groupId, request, leaderId);

        assertThat(response).isNotNull();
        verify(notificationService).notify(
                org.mockito.ArgumentMatchers.<List<UUID>>any(),
                org.mockito.ArgumentMatchers.eq(com.sep.treksphere.notification.NotificationEventType.GROUP_POST_ANNOUNCEMENT),
                org.mockito.ArgumentMatchers.eq(com.sep.treksphere.notification.ReferenceType.MATCHING_GROUP),
                org.mockito.ArgumentMatchers.eq(groupId),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("updateGroupPost - Tác giả cập nhật bài đăng và ảnh thành công")
    void updateGroupPost_SuccessByAuthor() {
        UUID postId = post.getGroupPostId();
        GroupPostUpdateRequest request = GroupPostUpdateRequest.builder()
                .title("Cập nhật lều")
                .content("Đã đủ lều")
                .imageUrls(List.of("https://res.cloudinary.com/demo/updated.jpg"))
                .build();

        when(matchingGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(matchingMemberRepository.findActiveMembers(groupId, JoinStatus.ACCEPTED)).thenReturn(List.of(authorMember));
        when(postRepository.findByGroupPostIdAndMatchingGroup_MatchingGroupIdAndIsDeletedFalse(postId, groupId))
                .thenReturn(Optional.of(post));
        when(postRepository.save(any(GroupPost.class))).thenAnswer(inv -> inv.getArgument(0));

        GroupPostResponse response = postService.updateGroupPost(groupId, postId, request, authorId);

        assertThat(response.getTitle()).isEqualTo("Cập nhật lều");
        assertThat(response.getContent()).isEqualTo("Đã đủ lều");
        assertThat(response.getImageUrls()).containsExactly("https://res.cloudinary.com/demo/updated.jpg");
        verify(postRepository).save(post);
    }

    @Test
    @DisplayName("updateGroupPost - Người khác không phải tác giả không được cập nhật bài đăng")
    void updateGroupPost_ForbiddenWhenNotAuthor() {
        UUID postId = post.getGroupPostId();
        GroupPostUpdateRequest request = GroupPostUpdateRequest.builder()
                .title("Cập nhật lều")
                .content("Đã đủ lều")
                .build();

        when(matchingGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(matchingMemberRepository.findActiveMembers(groupId, JoinStatus.ACCEPTED)).thenReturn(List.of(otherMember));
        when(postRepository.findByGroupPostIdAndMatchingGroup_MatchingGroupIdAndIsDeletedFalse(postId, groupId))
                .thenReturn(Optional.of(post));

        assertThatThrownBy(() -> postService.updateGroupPost(groupId, postId, request, otherMemberId))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.UNAUTHORIZED_POST_ACTION);
    }

    @Test
    @DisplayName("deleteGroupPost - Leader có quyền xóa bài đăng của thành viên")
    void deleteGroupPost_SuccessByLeader() {
        UUID postId = post.getGroupPostId();

        when(matchingGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(matchingMemberRepository.findActiveMembers(groupId, JoinStatus.ACCEPTED)).thenReturn(List.of(leaderMember));
        when(postRepository.findByGroupPostIdAndMatchingGroup_MatchingGroupIdAndIsDeletedFalse(postId, groupId))
                .thenReturn(Optional.of(post));

        postService.deleteGroupPost(groupId, postId, leaderId);

        assertThat(post.getIsDeleted()).isTrue();
        verify(postRepository).save(post);
    }

    @Test
    @DisplayName("deleteGroupPost - Thành viên khác không được xoá bài của tác giả")
    void deleteGroupPost_ForbiddenByOtherMember() {
        UUID postId = post.getGroupPostId();

        when(matchingGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(matchingMemberRepository.findActiveMembers(groupId, JoinStatus.ACCEPTED)).thenReturn(List.of(otherMember));
        when(postRepository.findByGroupPostIdAndMatchingGroup_MatchingGroupIdAndIsDeletedFalse(postId, groupId))
                .thenReturn(Optional.of(post));

        assertThatThrownBy(() -> postService.deleteGroupPost(groupId, postId, otherMemberId))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.UNAUTHORIZED_POST_ACTION);
    }

    @Test
    @DisplayName("toggleHideGroupPost - Leader ẩn/hiện bài đăng kiểm duyệt thành công")
    void toggleHideGroupPost_SuccessByLeader() {
        UUID postId = post.getGroupPostId();

        when(matchingGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(matchingMemberRepository.findActiveMembers(groupId, JoinStatus.ACCEPTED)).thenReturn(List.of(leaderMember));
        when(postRepository.findByGroupPostIdAndMatchingGroup_MatchingGroupIdAndIsDeletedFalse(postId, groupId))
                .thenReturn(Optional.of(post));
        when(postRepository.save(any(GroupPost.class))).thenAnswer(inv -> inv.getArgument(0));

        GroupPostResponse response = postService.toggleHideGroupPost(groupId, postId, leaderId);

        assertThat(response.getStatus()).isEqualTo(GroupContentStatus.HIDDEN);
        assertThat(post.getStatus()).isEqualTo(GroupContentStatus.HIDDEN);
    }

    @Test
    @DisplayName("createComment - Thành viên gửi bình luận thành công")
    void createComment_Success() {
        UUID postId = post.getGroupPostId();
        GroupPostCommentCreateRequest request = GroupPostCommentCreateRequest.builder()
                .content("Bình luận mới")
                .build();

        when(matchingGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(matchingMemberRepository.findActiveMembers(groupId, JoinStatus.ACCEPTED)).thenReturn(List.of(otherMember));
        when(postRepository.findByGroupPostIdAndMatchingGroup_MatchingGroupIdAndIsDeletedFalse(postId, groupId))
                .thenReturn(Optional.of(post));
        when(commentRepository.save(any(GroupPostComment.class))).thenAnswer(inv -> {
            GroupPostComment c = inv.getArgument(0);
            c.setGroupPostCommentId(UUID.randomUUID());
            return c;
        });

        GroupPostCommentResponse response = postService.createComment(groupId, postId, request, otherMemberId);

        assertThat(response).isNotNull();
        assertThat(response.getContent()).isEqualTo("Bình luận mới");
        verify(notificationService).notify(
                org.mockito.ArgumentMatchers.eq(authorMember.getUser().getUserId()),
                org.mockito.ArgumentMatchers.eq(com.sep.treksphere.notification.NotificationEventType.GROUP_POST_COMMENT_ADDED),
                org.mockito.ArgumentMatchers.eq(com.sep.treksphere.notification.ReferenceType.MATCHING_GROUP),
                org.mockito.ArgumentMatchers.eq(groupId),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("createComment - Trả lời bình luận gốc thành công")
    void createComment_ReplyToRootComment_Success() {
        UUID postId = post.getGroupPostId();
        UUID rootCommentId = comment.getGroupPostCommentId();
        GroupPostCommentCreateRequest request = GroupPostCommentCreateRequest.builder()
                .content("Trả lời bình luận gốc")
                .replyToCommentId(rootCommentId)
                .build();

        when(matchingGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(matchingMemberRepository.findActiveMembers(groupId, JoinStatus.ACCEPTED)).thenReturn(List.of(authorMember));
        when(postRepository.findByGroupPostIdAndMatchingGroup_MatchingGroupIdAndIsDeletedFalse(postId, groupId))
                .thenReturn(Optional.of(post));
        when(commentRepository.findByGroupPostCommentIdAndGroupPost_GroupPostIdAndIsDeletedFalse(rootCommentId, postId))
                .thenReturn(Optional.of(comment));
        when(commentRepository.save(any(GroupPostComment.class))).thenAnswer(inv -> {
            GroupPostComment c = inv.getArgument(0);
            c.setGroupPostCommentId(UUID.randomUUID());
            return c;
        });

        GroupPostCommentResponse response = postService.createComment(groupId, postId, request, authorId);

        assertThat(response).isNotNull();
        assertThat(response.getContent()).isEqualTo("Trả lời bình luận gốc");
        assertThat(response.getParentCommentId()).isEqualTo(rootCommentId);
        assertThat(response.getReplyToCommentId()).isEqualTo(rootCommentId);
        assertThat(response.getReplyToFullName()).isEqualTo("Other Member");
    }

    @Test
    @DisplayName("createComment - Trả lời một reply con tự động gán parent là root comment (giới hạn 2 cấp)")
    void createComment_ReplyToChildReply_FlattensToRoot() {
        UUID postId = post.getGroupPostId();
        UUID rootCommentId = comment.getGroupPostCommentId();

        GroupPostComment childReply = new GroupPostComment();
        childReply.setGroupPostCommentId(UUID.randomUUID());
        childReply.setGroupPost(post);
        childReply.setParentComment(comment);
        childReply.setAnsweredBy(authorMember);
        childReply.setContent("Đây là reply cấp 2");
        childReply.setStatus(GroupContentStatus.SHOW);
        childReply.setIsDeleted(false);

        UUID childReplyId = childReply.getGroupPostCommentId();

        GroupPostCommentCreateRequest request = GroupPostCommentCreateRequest.builder()
                .content("Phản hồi tiếp câu trả lời của tác giả")
                .replyToCommentId(childReplyId)
                .build();

        when(matchingGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(matchingMemberRepository.findActiveMembers(groupId, JoinStatus.ACCEPTED)).thenReturn(List.of(otherMember));
        when(postRepository.findByGroupPostIdAndMatchingGroup_MatchingGroupIdAndIsDeletedFalse(postId, groupId))
                .thenReturn(Optional.of(post));
        when(commentRepository.findByGroupPostCommentIdAndGroupPost_GroupPostIdAndIsDeletedFalse(childReplyId, postId))
                .thenReturn(Optional.of(childReply));
        when(commentRepository.save(any(GroupPostComment.class))).thenAnswer(inv -> {
            GroupPostComment c = inv.getArgument(0);
            c.setGroupPostCommentId(UUID.randomUUID());
            return c;
        });

        GroupPostCommentResponse response = postService.createComment(groupId, postId, request, otherMemberId);

        assertThat(response).isNotNull();
        // parentCommentId phải trỏ về root comment
        assertThat(response.getParentCommentId()).isEqualTo(rootCommentId);
        // replyToCommentId trỏ về childReply
        assertThat(response.getReplyToCommentId()).isEqualTo(childReplyId);
        assertThat(response.getReplyToFullName()).isEqualTo("Author User");
    }

    @Test
    @DisplayName("createComment - Trả lời comment không tồn tại ném AppException")
    void createComment_TargetNotFound_ThrowsException() {
        UUID postId = post.getGroupPostId();
        UUID fakeCommentId = UUID.randomUUID();
        GroupPostCommentCreateRequest request = GroupPostCommentCreateRequest.builder()
                .content("Bình luận lỗi")
                .replyToCommentId(fakeCommentId)
                .build();

        when(matchingGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(matchingMemberRepository.findActiveMembers(groupId, JoinStatus.ACCEPTED)).thenReturn(List.of(authorMember));
        when(postRepository.findByGroupPostIdAndMatchingGroup_MatchingGroupIdAndIsDeletedFalse(postId, groupId))
                .thenReturn(Optional.of(post));
        when(commentRepository.findByGroupPostCommentIdAndGroupPost_GroupPostIdAndIsDeletedFalse(fakeCommentId, postId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.createComment(groupId, postId, request, authorId))
                .isInstanceOf(AppException.class)
                .hasMessageContaining(ErrorCode.COMMENT_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("getGroupPostDetail - Trả về cây 2 cấp cho root comment và replies")
    void getGroupPostDetail_ReturnsHierarchy() {
        UUID postId = post.getGroupPostId();
        UUID rootCommentId = comment.getGroupPostCommentId();

        GroupPostComment reply = new GroupPostComment();
        reply.setGroupPostCommentId(UUID.randomUUID());
        reply.setGroupPost(post);
        reply.setParentComment(comment);
        reply.setAnsweredBy(authorMember);
        reply.setContent("Reply cho comment gốc");
        reply.setStatus(GroupContentStatus.SHOW);
        reply.setIsDeleted(false);

        when(matchingGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(matchingMemberRepository.findActiveMembers(groupId, JoinStatus.ACCEPTED)).thenReturn(List.of(authorMember));
        when(postRepository.findByGroupPostIdAndMatchingGroup_MatchingGroupIdAndIsDeletedFalse(postId, groupId))
                .thenReturn(Optional.of(post));
        when(commentRepository.findByGroupPost_GroupPostIdAndParentCommentIsNullAndStatusAndIsDeletedFalseOrderByCreatedAtAsc(
                postId, GroupContentStatus.SHOW)).thenReturn(List.of(comment));
        when(commentRepository.findByParentComment_GroupPostCommentIdAndStatusAndIsDeletedFalseOrderByCreatedAtAsc(
                rootCommentId, GroupContentStatus.SHOW)).thenReturn(List.of(reply));

        var detailResponse = postService.getGroupPostDetail(groupId, postId, authorId);


        assertThat(detailResponse).isNotNull();
        assertThat(detailResponse.getPost().getCommentCount()).isEqualTo(2);
        assertThat(detailResponse.getComments()).hasSize(1);
        assertThat(detailResponse.getComments().get(0).getReplies()).hasSize(1);
        assertThat(detailResponse.getComments().get(0).getReplies().get(0).getContent()).isEqualTo("Reply cho comment gốc");
    }

    @Test
    @DisplayName("updateComment - Tác giả cập nhật bình luận thành công")
    void updateComment_SuccessByAuthor() {
        UUID postId = post.getGroupPostId();
        UUID commentId = comment.getGroupPostCommentId();
        GroupPostCommentUpdateRequest request = GroupPostCommentUpdateRequest.builder()
                .content("Nội dung sửa đổi")
                .build();

        when(matchingGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(matchingMemberRepository.findActiveMembers(groupId, JoinStatus.ACCEPTED)).thenReturn(List.of(otherMember));
        when(commentRepository.findByGroupPostCommentIdAndGroupPost_GroupPostIdAndIsDeletedFalse(commentId, postId))
                .thenReturn(Optional.of(comment));
        when(commentRepository.save(any(GroupPostComment.class))).thenAnswer(inv -> inv.getArgument(0));

        GroupPostCommentResponse response = postService.updateComment(groupId, postId, commentId, request, otherMemberId);

        assertThat(response.getContent()).isEqualTo("Nội dung sửa đổi");
        verify(commentRepository).save(comment);
    }

    @Test
    @DisplayName("deleteComment - Leader xóa bình luận của thành viên và cascade xóa replies")
    void deleteComment_SuccessByLeader_WithCascadeReplies() {
        UUID postId = post.getGroupPostId();
        UUID commentId = comment.getGroupPostCommentId();

        GroupPostComment childReply = new GroupPostComment();
        childReply.setGroupPostCommentId(UUID.randomUUID());
        childReply.setGroupPost(post);
        childReply.setParentComment(comment);
        childReply.setIsDeleted(false);

        when(matchingGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(matchingMemberRepository.findActiveMembers(groupId, JoinStatus.ACCEPTED)).thenReturn(List.of(leaderMember));
        when(commentRepository.findByGroupPostCommentIdAndGroupPost_GroupPostIdAndIsDeletedFalse(commentId, postId))
                .thenReturn(Optional.of(comment));
        when(commentRepository.findByParentComment_GroupPostCommentIdAndIsDeletedFalse(commentId))
                .thenReturn(List.of(childReply));

        postService.deleteComment(groupId, postId, commentId, leaderId);

        assertThat(comment.getIsDeleted()).isTrue();
        assertThat(childReply.getIsDeleted()).isTrue();
        verify(commentRepository).save(comment);
        verify(commentRepository).saveAll(anyList());
    }
}
