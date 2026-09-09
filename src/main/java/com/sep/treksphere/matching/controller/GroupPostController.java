package com.sep.treksphere.matching.controller;

import com.sep.treksphere.common.constant.MessageConstant;
import com.sep.treksphere.common.dto.ApiResponse;
import com.sep.treksphere.common.security.CustomUserDetails;
import com.sep.treksphere.matching.dto.request.GroupPostCommentCreateRequest;
import com.sep.treksphere.matching.dto.request.GroupPostCommentUpdateRequest;
import com.sep.treksphere.matching.dto.request.GroupPostCreateRequest;
import com.sep.treksphere.matching.dto.request.GroupPostUpdateRequest;
import com.sep.treksphere.matching.dto.response.GroupPostCommentResponse;
import com.sep.treksphere.matching.dto.response.GroupPostDetailResponse;
import com.sep.treksphere.matching.dto.response.GroupPostResponse;
import com.sep.treksphere.matching.service.GroupPostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/matching-groups/{groupId}/posts")
@RequiredArgsConstructor
@Tag(name = "Matching Group Feed & Posts", description = "Quản lý bảng tin trao đổi & bình luận nội bộ nhóm ghép")
public class GroupPostController {

    private final GroupPostService postService;

    @GetMapping
    @Operation(summary = "Lấy danh sách bài đăng bảng tin của nhóm (phân trang)")
    public ResponseEntity<ApiResponse<Page<GroupPostResponse>>> getGroupPosts(
            @PathVariable UUID groupId,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        UUID currentUserId = userDetails != null ? userDetails.getUser().getUserId() : null;
        Page<GroupPostResponse> posts = postService.getGroupPosts(groupId, pageable, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, posts));
    }

    @GetMapping("/{postId}")
    @Operation(summary = "Lấy chi tiết bài đăng cùng danh sách bình luận")
    public ResponseEntity<ApiResponse<GroupPostDetailResponse>> getGroupPostDetail(
            @PathVariable UUID groupId,
            @PathVariable UUID postId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        UUID currentUserId = userDetails != null ? userDetails.getUser().getUserId() : null;
        GroupPostDetailResponse detail = postService.getGroupPostDetail(groupId, postId, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, detail));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('MATCHING_GROUP_PARTICIPATE')")
    @Operation(summary = "Đăng bài viết mới trong nhóm ghép")
    public ResponseEntity<ApiResponse<GroupPostResponse>> createGroupPost(
            @PathVariable UUID groupId,
            @Valid @RequestBody GroupPostCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        UUID currentUserId = userDetails.getUser().getUserId();
        GroupPostResponse post = postService.createGroupPost(groupId, request, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(HttpStatus.CREATED, post, MessageConstant.POST_CREATED_SUCCESS));
    }

    @PutMapping("/{postId}")
    @PreAuthorize("hasAuthority('MATCHING_GROUP_PARTICIPATE')")
    @Operation(summary = "Chỉnh sửa bài viết (chỉ tác giả)")
    public ResponseEntity<ApiResponse<GroupPostResponse>> updateGroupPost(
            @PathVariable UUID groupId,
            @PathVariable UUID postId,
            @Valid @RequestBody GroupPostUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        UUID currentUserId = userDetails.getUser().getUserId();
        GroupPostResponse post = postService.updateGroupPost(groupId, postId, request, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, post, MessageConstant.POST_UPDATED_SUCCESS));
    }

    @DeleteMapping("/{postId}")
    @PreAuthorize("hasAuthority('MATCHING_GROUP_PARTICIPATE')")
    @Operation(summary = "Xoá mềm bài viết (tác giả hoặc Leader)")
    public ResponseEntity<ApiResponse<Void>> deleteGroupPost(
            @PathVariable UUID groupId,
            @PathVariable UUID postId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        UUID currentUserId = userDetails.getUser().getUserId();
        postService.deleteGroupPost(groupId, postId, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, null, MessageConstant.POST_DELETED_SUCCESS));
    }

    @PatchMapping("/{postId}/toggle-hide")
    @PreAuthorize("hasAuthority('MATCHING_GROUP_PARTICIPATE')")
    @Operation(summary = "Ẩn/Hiện bài viết (kiểm duyệt - chỉ Leader)")
    public ResponseEntity<ApiResponse<GroupPostResponse>> toggleHideGroupPost(
            @PathVariable UUID groupId,
            @PathVariable UUID postId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        UUID currentUserId = userDetails.getUser().getUserId();
        GroupPostResponse post = postService.toggleHideGroupPost(groupId, postId, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, post, MessageConstant.POST_HIDDEN_SUCCESS));
    }

    @PostMapping("/{postId}/comments")
    @PreAuthorize("hasAuthority('MATCHING_GROUP_PARTICIPATE')")
    @Operation(summary = "Gửi bình luận vào bài viết")
    public ResponseEntity<ApiResponse<GroupPostCommentResponse>> createComment(
            @PathVariable UUID groupId,
            @PathVariable UUID postId,
            @Valid @RequestBody GroupPostCommentCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        UUID currentUserId = userDetails.getUser().getUserId();
        GroupPostCommentResponse comment = postService.createComment(groupId, postId, request, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(HttpStatus.CREATED, comment, MessageConstant.COMMENT_CREATED_SUCCESS));
    }

    @PutMapping("/{postId}/comments/{commentId}")
    @PreAuthorize("hasAuthority('MATCHING_GROUP_PARTICIPATE')")
    @Operation(summary = "Chỉnh sửa bình luận (chỉ tác giả)")
    public ResponseEntity<ApiResponse<GroupPostCommentResponse>> updateComment(
            @PathVariable UUID groupId,
            @PathVariable UUID postId,
            @PathVariable UUID commentId,
            @Valid @RequestBody GroupPostCommentUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        UUID currentUserId = userDetails.getUser().getUserId();
        GroupPostCommentResponse comment = postService.updateComment(groupId, postId, commentId, request, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, comment, MessageConstant.COMMENT_UPDATED_SUCCESS));
    }

    @DeleteMapping("/{postId}/comments/{commentId}")
    @PreAuthorize("hasAuthority('MATCHING_GROUP_PARTICIPATE')")
    @Operation(summary = "Xoá mềm bình luận (tác giả hoặc Leader)")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @PathVariable UUID groupId,
            @PathVariable UUID postId,
            @PathVariable UUID commentId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        UUID currentUserId = userDetails.getUser().getUserId();
        postService.deleteComment(groupId, postId, commentId, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, null, MessageConstant.COMMENT_DELETED_SUCCESS));
    }

    @PatchMapping("/{postId}/comments/{commentId}/toggle-hide")
    @PreAuthorize("hasAuthority('MATCHING_GROUP_PARTICIPATE')")
    @Operation(summary = "Ẩn/Hiện bình luận (kiểm duyệt - chỉ Leader)")
    public ResponseEntity<ApiResponse<GroupPostCommentResponse>> toggleHideComment(
            @PathVariable UUID groupId,
            @PathVariable UUID postId,
            @PathVariable UUID commentId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        UUID currentUserId = userDetails.getUser().getUserId();
        GroupPostCommentResponse comment = postService.toggleHideComment(groupId, postId, commentId, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, comment, MessageConstant.COMMENT_HIDDEN_SUCCESS));
    }
}
