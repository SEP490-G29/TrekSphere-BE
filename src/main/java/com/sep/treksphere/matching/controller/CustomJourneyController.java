package com.sep.treksphere.matching.controller;

import com.sep.treksphere.common.constant.MessageConstant;
import com.sep.treksphere.common.dto.ApiResponse;
import com.sep.treksphere.common.security.CustomUserDetails;
import com.sep.treksphere.matching.dto.request.CustomJourneyCheckpointCreateRequest;
import com.sep.treksphere.matching.dto.request.CustomJourneyCheckpointUpdateRequest;
import com.sep.treksphere.matching.dto.request.CustomJourneyUpdateRequest;
import com.sep.treksphere.matching.dto.response.CustomJourneyCheckpointResponse;
import com.sep.treksphere.matching.dto.response.CustomJourneyDetailResponse;
import com.sep.treksphere.matching.service.CustomJourneyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/matching-groups/{groupId}/journey")
@RequiredArgsConstructor
@Tag(name = "Matching Group Custom Journey", description = "Các API quản lý lịch trình và điểm dừng của nhóm ghép")
public class CustomJourneyController {

    private final CustomJourneyService customJourneyService;

    @Operation(summary = "Xem thông tin hành trình Custom Journey của nhóm")
    @GetMapping
    public ResponseEntity<ApiResponse<CustomJourneyDetailResponse>> getJourney(
            @Parameter(description = "ID nhóm ghép") @PathVariable UUID groupId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        UUID currentUserId = userDetails != null ? userDetails.getUser().getUserId() : null;
        CustomJourneyDetailResponse response = customJourneyService.getJourneyByGroupId(groupId, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response));
    }

    @Operation(summary = "Cập nhật thông tin tổng quan Custom Journey (chỉ Leader khi chưa khóa)")
    @PutMapping
    @PreAuthorize("hasAuthority('MATCHING_GROUP_MANAGE_OWN')")
    public ResponseEntity<ApiResponse<CustomJourneyDetailResponse>> updateJourney(
            @Parameter(description = "ID nhóm ghép") @PathVariable UUID groupId,
            @Valid @RequestBody CustomJourneyUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        CustomJourneyDetailResponse response = customJourneyService.updateJourney(
                groupId, request, userDetails.getUser().getUserId());
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response, MessageConstant.JOURNEY_UPDATED_SUCCESS));
    }

    @Operation(summary = "Lấy danh sách các điểm dừng/hoạt động theo ngày")
    @GetMapping("/checkpoints")
    public ResponseEntity<ApiResponse<List<CustomJourneyCheckpointResponse>>> getCheckpoints(
            @Parameter(description = "ID nhóm ghép") @PathVariable UUID groupId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        UUID currentUserId = userDetails != null ? userDetails.getUser().getUserId() : null;
        List<CustomJourneyCheckpointResponse> response = customJourneyService.getCheckpoints(groupId, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response));
    }

    @Operation(summary = "Thêm điểm dừng mới vào hành trình (chỉ Leader khi chưa khóa)")
    @PostMapping("/checkpoints")
    @PreAuthorize("hasAuthority('MATCHING_GROUP_MANAGE_OWN')")
    public ResponseEntity<ApiResponse<CustomJourneyCheckpointResponse>> createCheckpoint(
            @Parameter(description = "ID nhóm ghép") @PathVariable UUID groupId,
            @Valid @RequestBody CustomJourneyCheckpointCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        CustomJourneyCheckpointResponse response = customJourneyService.createCheckpoint(
                groupId, request, userDetails.getUser().getUserId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED, response, MessageConstant.CHECKPOINT_CREATED_SUCCESS));
    }

    @Operation(summary = "Cập nhật điểm dừng trong hành trình (chỉ Leader khi chưa khóa)")
    @PutMapping("/checkpoints/{checkpointId}")
    @PreAuthorize("hasAuthority('MATCHING_GROUP_MANAGE_OWN')")
    public ResponseEntity<ApiResponse<CustomJourneyCheckpointResponse>> updateCheckpoint(
            @Parameter(description = "ID nhóm ghép") @PathVariable UUID groupId,
            @Parameter(description = "ID điểm dừng checkpoint") @PathVariable UUID checkpointId,
            @Valid @RequestBody CustomJourneyCheckpointUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        CustomJourneyCheckpointResponse response = customJourneyService.updateCheckpoint(
                groupId, checkpointId, request, userDetails.getUser().getUserId());
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response, MessageConstant.CHECKPOINT_UPDATED_SUCCESS));
    }

    @Operation(summary = "Xoá điểm dừng khỏi hành trình (chỉ Leader khi chưa khóa)")
    @DeleteMapping("/checkpoints/{checkpointId}")
    @PreAuthorize("hasAuthority('MATCHING_GROUP_MANAGE_OWN')")
    public ResponseEntity<ApiResponse<Void>> deleteCheckpoint(
            @Parameter(description = "ID nhóm ghép") @PathVariable UUID groupId,
            @Parameter(description = "ID điểm dừng checkpoint") @PathVariable UUID checkpointId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        customJourneyService.deleteCheckpoint(groupId, checkpointId, userDetails.getUser().getUserId());
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, null, MessageConstant.CHECKPOINT_DELETED_SUCCESS));
    }
}
