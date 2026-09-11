package com.sep.treksphere.matching.service.impl;

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
import com.sep.treksphere.matching.service.GroupPostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class GroupPostServiceImpl implements GroupPostService {

    private final GroupPostRepository postRepository;
    private final GroupPostCommentRepository commentRepository;
    private final MatchingGroupRepository matchingGroupRepository;
    private final MatchingMemberRepository matchingMemberRepository;
    private final GroupPostMapper postMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<GroupPostResponse> getGroupPosts(UUID groupId, Pageable pageable, UUID currentUserId) {
        MatchingGroup group = getGroupOrThrow(groupId);
        validateReadPermission(group, currentUserId);

        boolean isLeader = isGroupLeader(groupId, currentUserId);

        Page<GroupPost> posts;
        if (isLeader) {
            posts = postRepository.findByMatchingGroup_MatchingGroupIdAndIsDeletedFalse(groupId, pageable);
        } else {
            posts = postRepository.findByMatchingGroup_MatchingGroupIdAndStatusAndIsDeletedFalse(
                    groupId, GroupContentStatus.SHOW, pageable);
        }

        return posts.map(post -> {
            GroupPostResponse response = postMapper.toPostResponse(post);
            long count = isLeader
                    ? commentRepository.countByGroupPost_GroupPostIdAndIsDeletedFalse(post.getGroupPostId())
                    : commentRepository.countByGroupPost_GroupPostIdAndStatusAndIsDeletedFalse(
                            post.getGroupPostId(), GroupContentStatus.SHOW);
            response.setCommentCount(count);
            return response;
        });
    }

    @Override
    @Transactional(readOnly = true)
    public GroupPostDetailResponse getGroupPostDetail(UUID groupId, UUID postId, UUID currentUserId) {
        MatchingGroup group = getGroupOrThrow(groupId);
        validateReadPermission(group, currentUserId);

        GroupPost post = postRepository
                .findByGroupPostIdAndMatchingGroup_MatchingGroupIdAndIsDeletedFalse(postId, groupId)
                .orElseThrow(() -> new AppException(ErrorCode.POST_NOT_FOUND));

        boolean isLeader = isGroupLeader(groupId, currentUserId);
        boolean isAuthor = isPostAuthor(post, currentUserId);

        if (post.getStatus() == GroupContentStatus.HIDDEN && !isLeader && !isAuthor) {
            throw new AppException(ErrorCode.POST_NOT_FOUND);
        }

        List<GroupPostComment> rootComments;
        if (isLeader) {
            rootComments = commentRepository.findByGroupPost_GroupPostIdAndParentCommentIsNullAndIsDeletedFalseOrderByCreatedAtAsc(postId);
        } else {
            rootComments = commentRepository.findByGroupPost_GroupPostIdAndParentCommentIsNullAndStatusAndIsDeletedFalseOrderByCreatedAtAsc(
                    postId, GroupContentStatus.SHOW);
        }

        List<GroupPostCommentResponse> commentResponses = new ArrayList<>();
        for (GroupPostComment root : rootComments) {
            GroupPostCommentResponse rootResponse = postMapper.toCommentResponse(root);
            List<GroupPostComment> replies;
            if (isLeader) {
                replies = commentRepository.findByParentComment_GroupPostCommentIdAndIsDeletedFalseOrderByCreatedAtAsc(root.getGroupPostCommentId());
            } else {
                replies = commentRepository.findByParentComment_GroupPostCommentIdAndStatusAndIsDeletedFalseOrderByCreatedAtAsc(
                        root.getGroupPostCommentId(), GroupContentStatus.SHOW);
            }
            rootResponse.setReplies(postMapper.toCommentResponseList(replies));
            commentResponses.add(rootResponse);
        }

        long totalComments;
        if (isLeader) {
            totalComments = commentRepository.countByGroupPost_GroupPostIdAndIsDeletedFalse(postId);
        } else {
            totalComments = commentResponses.stream()
                    .mapToLong(root -> 1 + (root.getReplies() != null ? root.getReplies().size() : 0))
                    .sum();
        }

        GroupPostResponse postResponse = postMapper.toPostResponse(post);
        postResponse.setCommentCount(totalComments);

        return GroupPostDetailResponse.builder()
                .post(postResponse)
                .comments(commentResponses)
                .build();
    }


    @Override
    @Transactional
    public GroupPostResponse createGroupPost(UUID groupId, GroupPostCreateRequest request, UUID currentUserId) {
        MatchingGroup group = getGroupOrThrow(groupId);
        MatchingMember callerMember = getCallerMemberOrThrow(groupId, currentUserId);

        GroupPost post = postMapper.toPostEntity(request);
        post.setMatchingGroup(group);
        post.setPostedBy(callerMember);
        post.setStatus(GroupContentStatus.SHOW);

        GroupPost saved = postRepository.save(post);
        log.info("Created post {} in group {} by user {}", saved.getGroupPostId(), groupId, currentUserId);

        GroupPostResponse response = postMapper.toPostResponse(saved);
        response.setCommentCount(0);
        return response;
    }

    @Override
    @Transactional
    public GroupPostResponse updateGroupPost(
            UUID groupId, UUID postId, GroupPostUpdateRequest request, UUID currentUserId) {
        getGroupOrThrow(groupId);
        getCallerMemberOrThrow(groupId, currentUserId);

        GroupPost post = postRepository
                .findByGroupPostIdAndMatchingGroup_MatchingGroupIdAndIsDeletedFalse(postId, groupId)
                .orElseThrow(() -> new AppException(ErrorCode.POST_NOT_FOUND));

        if (!isPostAuthor(post, currentUserId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED_POST_ACTION);
        }

        postMapper.updatePostEntityFromRequest(request, post);
        GroupPost saved = postRepository.save(post);
        log.info("Updated post {} in group {} by author {}", postId, groupId, currentUserId);

        GroupPostResponse response = postMapper.toPostResponse(saved);
        response.setCommentCount(commentRepository.countByGroupPost_GroupPostIdAndIsDeletedFalse(postId));
        return response;
    }

    @Override
    @Transactional
    public void deleteGroupPost(UUID groupId, UUID postId, UUID currentUserId) {
        getGroupOrThrow(groupId);
        MatchingMember callerMember = getCallerMemberOrThrow(groupId, currentUserId);

        GroupPost post = postRepository
                .findByGroupPostIdAndMatchingGroup_MatchingGroupIdAndIsDeletedFalse(postId, groupId)
                .orElseThrow(() -> new AppException(ErrorCode.POST_NOT_FOUND));

        boolean isAuthor = isPostAuthor(post, currentUserId);
        boolean isLeader = callerMember.getRole() == MatchingRole.LEADER;

        if (!isAuthor && !isLeader) {
            throw new AppException(ErrorCode.UNAUTHORIZED_POST_ACTION);
        }

        post.setIsDeleted(true);
        postRepository.save(post);
        log.info("Deleted post {} in group {} by user {}", postId, groupId, currentUserId);
    }

    @Override
    @Transactional
    public GroupPostResponse toggleHideGroupPost(UUID groupId, UUID postId, UUID currentUserId) {
        getGroupOrThrow(groupId);
        MatchingMember callerMember = getCallerMemberOrThrow(groupId, currentUserId);

        if (callerMember.getRole() != MatchingRole.LEADER) {
            throw new AppException(ErrorCode.UNAUTHORIZED_POST_ACTION);
        }

        GroupPost post = postRepository
                .findByGroupPostIdAndMatchingGroup_MatchingGroupIdAndIsDeletedFalse(postId, groupId)
                .orElseThrow(() -> new AppException(ErrorCode.POST_NOT_FOUND));

        GroupContentStatus newStatus = post.getStatus() == GroupContentStatus.SHOW
                ? GroupContentStatus.HIDDEN
                : GroupContentStatus.SHOW;
        post.setStatus(newStatus);
        GroupPost saved = postRepository.save(post);
        log.info("Leader {} changed post {} status to {}", currentUserId, postId, newStatus);

        GroupPostResponse response = postMapper.toPostResponse(saved);
        response.setCommentCount(commentRepository.countByGroupPost_GroupPostIdAndIsDeletedFalse(postId));
        return response;
    }

    @Override
    @Transactional
    public GroupPostCommentResponse createComment(
            UUID groupId, UUID postId, GroupPostCommentCreateRequest request, UUID currentUserId) {
        getGroupOrThrow(groupId);
        MatchingMember callerMember = getCallerMemberOrThrow(groupId, currentUserId);

        GroupPost post = postRepository
                .findByGroupPostIdAndMatchingGroup_MatchingGroupIdAndIsDeletedFalse(postId, groupId)
                .orElseThrow(() -> new AppException(ErrorCode.POST_NOT_FOUND));

        GroupPostComment comment = postMapper.toCommentEntity(request);
        comment.setGroupPost(post);
        comment.setAnsweredBy(callerMember);
        comment.setStatus(GroupContentStatus.SHOW);

        if (request.getReplyToCommentId() != null) {
            GroupPostComment targetComment = commentRepository
                    .findByGroupPostCommentIdAndGroupPost_GroupPostIdAndIsDeletedFalse(request.getReplyToCommentId(), postId)
                    .filter(c -> c.getGroupPost().getMatchingGroup().getMatchingGroupId().equals(groupId))
                    .orElseThrow(() -> new AppException(ErrorCode.COMMENT_NOT_FOUND));

            // Enforce max 2-level hierarchy: if target has parent, root is target's parent; otherwise target is root
            GroupPostComment rootComment = targetComment.getParentComment() != null
                    ? targetComment.getParentComment()
                    : targetComment;

            comment.setParentComment(rootComment);
            comment.setReplyToComment(targetComment);
            comment.setReplyToMember(targetComment.getAnsweredBy());
        }

        GroupPostComment saved = commentRepository.save(comment);
        log.info("Created comment {} (replyTo: {}) for post {} by user {}",
                saved.getGroupPostCommentId(), request.getReplyToCommentId(), postId, currentUserId);
        return postMapper.toCommentResponse(saved);
    }

    @Override
    @Transactional
    public GroupPostCommentResponse updateComment(
            UUID groupId, UUID postId, UUID commentId, GroupPostCommentUpdateRequest request, UUID currentUserId) {
        getGroupOrThrow(groupId);
        getCallerMemberOrThrow(groupId, currentUserId);

        GroupPostComment comment = commentRepository
                .findByGroupPostCommentIdAndGroupPost_GroupPostIdAndIsDeletedFalse(commentId, postId)
                .filter(c -> c.getGroupPost().getMatchingGroup().getMatchingGroupId().equals(groupId))
                .orElseThrow(() -> new AppException(ErrorCode.COMMENT_NOT_FOUND));

        if (!isCommentAuthor(comment, currentUserId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED_COMMENT_ACTION);
        }

        postMapper.updateCommentEntityFromRequest(request, comment);
        GroupPostComment saved = commentRepository.save(comment);
        log.info("Updated comment {} on post {} by author {}", commentId, postId, currentUserId);
        return postMapper.toCommentResponse(saved);
    }

    @Override
    @Transactional
    public void deleteComment(UUID groupId, UUID postId, UUID commentId, UUID currentUserId) {
        getGroupOrThrow(groupId);
        MatchingMember callerMember = getCallerMemberOrThrow(groupId, currentUserId);

        GroupPostComment comment = commentRepository
                .findByGroupPostCommentIdAndGroupPost_GroupPostIdAndIsDeletedFalse(commentId, postId)
                .filter(c -> c.getGroupPost().getMatchingGroup().getMatchingGroupId().equals(groupId))
                .orElseThrow(() -> new AppException(ErrorCode.COMMENT_NOT_FOUND));

        boolean isAuthor = isCommentAuthor(comment, currentUserId);
        boolean isLeader = callerMember.getRole() == MatchingRole.LEADER;

        if (!isAuthor && !isLeader) {
            throw new AppException(ErrorCode.UNAUTHORIZED_COMMENT_ACTION);
        }

        comment.setIsDeleted(true);
        commentRepository.save(comment);

        // Cascade soft-delete child replies if this was a root comment
        if (comment.getParentComment() == null) {
            List<GroupPostComment> childReplies = commentRepository
                    .findByParentComment_GroupPostCommentIdAndIsDeletedFalse(commentId);
            if (!childReplies.isEmpty()) {
                childReplies.forEach(child -> child.setIsDeleted(true));
                commentRepository.saveAll(childReplies);
                log.info("Cascade soft-deleted {} replies under root comment {}", childReplies.size(), commentId);
            }
        }

        log.info("Deleted comment {} on post {} by user {}", commentId, postId, currentUserId);
    }

    @Override
    @Transactional
    public GroupPostCommentResponse toggleHideComment(
            UUID groupId, UUID postId, UUID commentId, UUID currentUserId) {
        getGroupOrThrow(groupId);
        MatchingMember callerMember = getCallerMemberOrThrow(groupId, currentUserId);

        if (callerMember.getRole() != MatchingRole.LEADER) {
            throw new AppException(ErrorCode.UNAUTHORIZED_COMMENT_ACTION);
        }

        GroupPostComment comment = commentRepository
                .findByGroupPostCommentIdAndGroupPost_GroupPostIdAndIsDeletedFalse(commentId, postId)
                .filter(c -> c.getGroupPost().getMatchingGroup().getMatchingGroupId().equals(groupId))
                .orElseThrow(() -> new AppException(ErrorCode.COMMENT_NOT_FOUND));

        GroupContentStatus newStatus = comment.getStatus() == GroupContentStatus.SHOW
                ? GroupContentStatus.HIDDEN
                : GroupContentStatus.SHOW;
        comment.setStatus(newStatus);
        GroupPostComment saved = commentRepository.save(comment);

        // Cascade update status for child replies if this is a root comment
        if (comment.getParentComment() == null) {
            List<GroupPostComment> childReplies = commentRepository
                    .findByParentComment_GroupPostCommentIdAndIsDeletedFalse(commentId);
            if (!childReplies.isEmpty()) {
                childReplies.forEach(child -> child.setStatus(newStatus));
                commentRepository.saveAll(childReplies);
                log.info("Cascade changed {} replies under root comment {} to status {}",
                        childReplies.size(), commentId, newStatus);
            }
        }

        log.info("Leader {} changed comment {} status to {}", currentUserId, commentId, newStatus);
        return postMapper.toCommentResponse(saved);
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

    private boolean isGroupLeader(UUID groupId, UUID currentUserId) {
        if (currentUserId == null) {
            return false;
        }
        return matchingMemberRepository.findActiveMembers(groupId, JoinStatus.ACCEPTED)
                .stream()
                .anyMatch(m -> m.getUser().getUserId().equals(currentUserId) && m.getRole() == MatchingRole.LEADER);
    }

    private boolean isPostAuthor(GroupPost post, UUID currentUserId) {
        return currentUserId != null
                && post.getPostedBy() != null
                && post.getPostedBy().getUser() != null
                && currentUserId.equals(post.getPostedBy().getUser().getUserId());
    }

    private boolean isCommentAuthor(GroupPostComment comment, UUID currentUserId) {
        return currentUserId != null
                && comment.getAnsweredBy() != null
                && comment.getAnsweredBy().getUser() != null
                && currentUserId.equals(comment.getAnsweredBy().getUser().getUserId());
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
