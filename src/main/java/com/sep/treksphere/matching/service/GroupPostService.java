package com.sep.treksphere.matching.service;

import com.sep.treksphere.matching.dto.request.GroupPostCommentCreateRequest;
import com.sep.treksphere.matching.dto.request.GroupPostCommentUpdateRequest;
import com.sep.treksphere.matching.dto.request.GroupPostCreateRequest;
import com.sep.treksphere.matching.dto.request.GroupPostUpdateRequest;
import com.sep.treksphere.matching.dto.response.GroupPostCommentResponse;
import com.sep.treksphere.matching.dto.response.GroupPostDetailResponse;
import com.sep.treksphere.matching.dto.response.GroupPostResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface GroupPostService {

    Page<GroupPostResponse> getGroupPosts(UUID groupId, Pageable pageable, UUID currentUserId);

    GroupPostDetailResponse getGroupPostDetail(UUID groupId, UUID postId, UUID currentUserId);

    GroupPostResponse createGroupPost(UUID groupId, GroupPostCreateRequest request, UUID currentUserId);

    GroupPostResponse updateGroupPost(UUID groupId, UUID postId, GroupPostUpdateRequest request, UUID currentUserId);

    void deleteGroupPost(UUID groupId, UUID postId, UUID currentUserId);

    GroupPostResponse toggleHideGroupPost(UUID groupId, UUID postId, UUID currentUserId);

    GroupPostCommentResponse createComment(UUID groupId, UUID postId, GroupPostCommentCreateRequest request, UUID currentUserId);

    GroupPostCommentResponse updateComment(UUID groupId, UUID postId, UUID commentId, GroupPostCommentUpdateRequest request, UUID currentUserId);

    void deleteComment(UUID groupId, UUID postId, UUID commentId, UUID currentUserId);

    GroupPostCommentResponse toggleHideComment(UUID groupId, UUID postId, UUID commentId, UUID currentUserId);
}
