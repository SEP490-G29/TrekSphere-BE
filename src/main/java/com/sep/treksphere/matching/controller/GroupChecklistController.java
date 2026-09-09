package com.sep.treksphere.matching.controller;

import com.sep.treksphere.common.constant.MessageConstant;
import com.sep.treksphere.common.dto.ApiResponse;
import com.sep.treksphere.common.security.CustomUserDetails;
import com.sep.treksphere.matching.dto.request.GroupChecklistFilterRequest;
import com.sep.treksphere.matching.dto.request.GroupChecklistItemCreateRequest;
import com.sep.treksphere.matching.dto.request.GroupChecklistItemStatusUpdateRequest;
import com.sep.treksphere.matching.dto.request.GroupChecklistItemUpdateRequest;
import com.sep.treksphere.matching.dto.response.GroupChecklistItemResponse;
import com.sep.treksphere.matching.dto.response.GroupChecklistSummaryResponse;
import com.sep.treksphere.matching.service.GroupChecklistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/matching-groups/{groupId}/checklist-items")
@RequiredArgsConstructor
@Tag(name = "Matching Group Checklist", description = "Quản lý danh sách chuẩn bị (Checklist) của nhóm ghép")
public class GroupChecklistController {

    private final GroupChecklistService checklistService;

    @GetMapping
    @Operation(summary = "Lấy danh sách và thống kê checklist của nhóm ghép")
    public ResponseEntity<ApiResponse<GroupChecklistSummaryResponse>> getChecklistSummary(
            @PathVariable UUID groupId,
            @ModelAttribute GroupChecklistFilterRequest filter,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        UUID currentUserId = userDetails != null ? userDetails.getUser().getUserId() : null;
        GroupChecklistSummaryResponse response = checklistService.getChecklistSummary(groupId, filter, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('MATCHING_GROUP_PARTICIPATE')")
    @Operation(summary = "Tạo mới mục checklist trong nhóm ghép")
    public ResponseEntity<ApiResponse<GroupChecklistItemResponse>> createChecklistItem(
            @PathVariable UUID groupId,
            @Valid @RequestBody GroupChecklistItemCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        UUID currentUserId = userDetails.getUser().getUserId();
        GroupChecklistItemResponse response = checklistService.createChecklistItem(groupId, request, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(HttpStatus.CREATED, response, MessageConstant.CHECKLIST_ITEM_CREATED_SUCCESS));
    }

    @PutMapping("/{itemId}")
    @PreAuthorize("hasAuthority('MATCHING_GROUP_PARTICIPATE')")
    @Operation(summary = "Cập nhật thông tin mục checklist")
    public ResponseEntity<ApiResponse<GroupChecklistItemResponse>> updateChecklistItem(
            @PathVariable UUID groupId,
            @PathVariable UUID itemId,
            @Valid @RequestBody GroupChecklistItemUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        UUID currentUserId = userDetails.getUser().getUserId();
        GroupChecklistItemResponse response = checklistService.updateChecklistItem(groupId, itemId, request, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response, MessageConstant.CHECKLIST_ITEM_UPDATED_SUCCESS));
    }

    @PatchMapping("/{itemId}/status")
    @PreAuthorize("hasAuthority('MATCHING_GROUP_PARTICIPATE')")
    @Operation(summary = "Cập nhật trạng thái mục checklist")
    public ResponseEntity<ApiResponse<GroupChecklistItemResponse>> updateItemStatus(
            @PathVariable UUID groupId,
            @PathVariable UUID itemId,
            @Valid @RequestBody GroupChecklistItemStatusUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        UUID currentUserId = userDetails.getUser().getUserId();
        GroupChecklistItemResponse response = checklistService.updateItemStatus(groupId, itemId, request, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response, MessageConstant.CHECKLIST_ITEM_STATUS_UPDATED_SUCCESS));
    }

    @DeleteMapping("/{itemId}")
    @PreAuthorize("hasAuthority('MATCHING_GROUP_PARTICIPATE')")
    @Operation(summary = "Xoá mềm mục checklist")
    public ResponseEntity<ApiResponse<Void>> deleteChecklistItem(
            @PathVariable UUID groupId,
            @PathVariable UUID itemId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        UUID currentUserId = userDetails.getUser().getUserId();
        checklistService.deleteChecklistItem(groupId, itemId, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, null, MessageConstant.CHECKLIST_ITEM_DELETED_SUCCESS));
    }
}
