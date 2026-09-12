package com.sep.treksphere.matching.controller;

import com.sep.treksphere.common.constant.MessageConstant;
import com.sep.treksphere.common.dto.ApiResponse;
import com.sep.treksphere.common.dto.BaseFilterRequest;
import com.sep.treksphere.common.dto.PaginationResponse;
import com.sep.treksphere.common.security.CustomUserDetails;
import com.sep.treksphere.matching.dto.request.MomentCreateRequest;
import com.sep.treksphere.matching.dto.request.MomentFilterRequest;
import com.sep.treksphere.matching.dto.request.MomentHideRequest;
import com.sep.treksphere.matching.dto.request.MomentUpdateRequest;
import com.sep.treksphere.matching.dto.request.MomentVisibilityUpdateRequest;
import com.sep.treksphere.matching.dto.response.MomentMapResponse;
import com.sep.treksphere.matching.dto.response.MomentMediaResponse;
import com.sep.treksphere.matching.dto.response.MomentResponse;
import com.sep.treksphere.matching.service.MomentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/matching-groups/{groupId}/moments")
@RequiredArgsConstructor
@Tag(name = "Matching Group Moments", description = "Quản lý khoảnh khắc và album ảnh của nhóm ghép")
public class GroupMomentController {

    private final MomentService momentService;

    @PostMapping
    @PreAuthorize("hasAuthority('MATCHING_GROUP_PARTICIPATE')")
    @Operation(summary = "Đăng khoảnh khắc mới trong nhóm ghép")
    public ResponseEntity<ApiResponse<MomentResponse>> createMoment(
            @PathVariable UUID groupId,
            @Valid @RequestBody MomentCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        MomentResponse response = momentService.createGroupMoment(groupId, userDetails.getUser().getUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED, response, MessageConstant.MOMENT_CREATED_SUCCESS));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('MATCHING_GROUP_PARTICIPATE')")
    @Operation(summary = "Lấy dòng thời gian khoảnh khắc trong nhóm ghép")
    public ResponseEntity<ApiResponse<PaginationResponse<MomentResponse>>> getGroupMoments(
            @PathVariable UUID groupId,
            @Valid @ParameterObject @ModelAttribute MomentFilterRequest filter,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        PaginationResponse<MomentResponse> response = momentService.getGroupMoments(
                groupId, userDetails.getUser().getUserId(), filter);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response, MessageConstant.MOMENTS_FETCHED_SUCCESS));
    }

    @GetMapping("/album")
    @PreAuthorize("hasAuthority('MATCHING_GROUP_PARTICIPATE')")
    @Operation(summary = "Lấy album ảnh (lưới hình ảnh) trong nhóm ghép")
    public ResponseEntity<ApiResponse<PaginationResponse<MomentMediaResponse>>> getGroupAlbum(
            @PathVariable UUID groupId,
            @Valid @ParameterObject @ModelAttribute BaseFilterRequest filter,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        PaginationResponse<MomentMediaResponse> response = momentService.getGroupAlbum(
                groupId, userDetails.getUser().getUserId(), filter);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response, MessageConstant.MOMENTS_ALBUM_FETCHED_SUCCESS));
    }

    @GetMapping("/map")
    @PreAuthorize("hasAuthority('MATCHING_GROUP_PARTICIPATE')")
    @Operation(summary = "Lấy danh sách điểm ghim khoảnh khắc trên bản đồ nhóm")
    public ResponseEntity<ApiResponse<List<MomentMapResponse>>> getGroupMomentsMap(
            @PathVariable UUID groupId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<MomentMapResponse> response = momentService.getGroupMomentsMap(groupId, userDetails.getUser().getUserId());
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response, MessageConstant.MOMENTS_MAP_FETCHED_SUCCESS));
    }

    @GetMapping("/{momentId}")
    @PreAuthorize("hasAuthority('MATCHING_GROUP_PARTICIPATE')")
    @Operation(summary = "Xem chi tiết khoảnh khắc trong nhóm")
    public ResponseEntity<ApiResponse<MomentResponse>> getGroupMomentDetail(
            @PathVariable UUID groupId,
            @PathVariable UUID momentId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        MomentResponse response = momentService.getGroupMomentDetail(groupId, momentId, userDetails.getUser().getUserId());
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response, MessageConstant.MOMENT_FETCHED_SUCCESS));
    }

    @PutMapping("/{momentId}")
    @PreAuthorize("hasAuthority('MATCHING_GROUP_PARTICIPATE')")
    @Operation(summary = "Tác giả chỉnh sửa khoảnh khắc trong nhóm")
    public ResponseEntity<ApiResponse<MomentResponse>> updateMoment(
            @PathVariable UUID groupId,
            @PathVariable UUID momentId,
            @Valid @RequestBody MomentUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        MomentResponse response = momentService.updateGroupMoment(groupId, momentId, userDetails.getUser().getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response, MessageConstant.MOMENT_UPDATED_SUCCESS));
    }

    @DeleteMapping("/{momentId}")
    @PreAuthorize("hasAuthority('MATCHING_GROUP_PARTICIPATE')")
    @Operation(summary = "Tác giả xóa khoảnh khắc trong nhóm")
    public ResponseEntity<ApiResponse<Void>> deleteMoment(
            @PathVariable UUID groupId,
            @PathVariable UUID momentId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        momentService.deleteGroupMoment(groupId, momentId, userDetails.getUser().getUserId());
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, null, MessageConstant.MOMENT_DELETED_SUCCESS));
    }

    @PatchMapping("/{momentId}/visibility")
    @PreAuthorize("hasAuthority('MATCHING_GROUP_PARTICIPATE')")
    @Operation(summary = "Tác giả thay đổi quyền hiển thị khoảnh khắc (GROUP_ONLY / PUBLIC_PROFILE / ONLY_ME)")
    public ResponseEntity<ApiResponse<MomentResponse>> updateVisibility(
            @PathVariable UUID groupId,
            @PathVariable UUID momentId,
            @Valid @RequestBody MomentVisibilityUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        MomentResponse response = momentService.updateGroupMomentVisibility(
                groupId, momentId, userDetails.getUser().getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response, MessageConstant.MOMENT_VISIBILITY_UPDATED_SUCCESS));
    }

    @PostMapping("/{momentId}/hide")
    @PreAuthorize("hasAuthority('MATCHING_GROUP_PARTICIPATE')")
    @Operation(summary = "Leader ẩn khoảnh khắc vi phạm trong nhóm")
    public ResponseEntity<ApiResponse<MomentResponse>> hideMoment(
            @PathVariable UUID groupId,
            @PathVariable UUID momentId,
            @Valid @RequestBody MomentHideRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        MomentResponse response = momentService.hideGroupMoment(groupId, momentId, userDetails.getUser().getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response, MessageConstant.MOMENT_HIDDEN_SUCCESS));
    }

    @PostMapping("/{momentId}/unhide")
    @PreAuthorize("hasAuthority('MATCHING_GROUP_PARTICIPATE')")
    @Operation(summary = "Leader bỏ ẩn khoảnh khắc trong nhóm")
    public ResponseEntity<ApiResponse<MomentResponse>> unhideMoment(
            @PathVariable UUID groupId,
            @PathVariable UUID momentId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        MomentResponse response = momentService.unhideGroupMoment(groupId, momentId, userDetails.getUser().getUserId());
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response, MessageConstant.MOMENT_UNHIDDEN_SUCCESS));
    }
}
