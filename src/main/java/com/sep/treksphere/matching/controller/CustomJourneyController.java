package com.sep.treksphere.matching.controller;

import com.sep.treksphere.common.constant.MessageConstant;
import com.sep.treksphere.common.dto.ApiResponse;
import com.sep.treksphere.common.security.CustomUserDetails;
import com.sep.treksphere.matching.dto.request.CustomJourneyActivityCreateRequest;
import com.sep.treksphere.matching.dto.request.CustomJourneyActivityUpdateRequest;
import com.sep.treksphere.matching.dto.request.CustomJourneyCheckpointCreateRequest;
import com.sep.treksphere.matching.dto.request.CustomJourneyCheckpointUpdateRequest;
import com.sep.treksphere.matching.dto.request.CustomJourneyUpdateRequest;
import com.sep.treksphere.matching.dto.response.CustomJourneyActivityResponse;
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
    @PreAuthorize("isAuthenticated()")
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
    @PreAuthorize("isAuthenticated()")
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
    @PreAuthorize("isAuthenticated()")
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
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> deleteCheckpoint(
            @Parameter(description = "ID nhóm ghép") @PathVariable UUID groupId,
            @Parameter(description = "ID điểm dừng checkpoint") @PathVariable UUID checkpointId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        customJourneyService.deleteCheckpoint(groupId, checkpointId, userDetails.getUser().getUserId());
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, null, MessageConstant.CHECKPOINT_DELETED_SUCCESS));
    }

    @Operation(summary = "Lấy danh sách các hoạt động trong thời khóa biểu hành trình")
    @GetMapping("/activities")
    public ResponseEntity<ApiResponse<List<CustomJourneyActivityResponse>>> getActivities(
            @Parameter(description = "ID nhóm ghép") @PathVariable UUID groupId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        UUID currentUserId = userDetails != null ? userDetails.getUser().getUserId() : null;
        List<CustomJourneyActivityResponse> response = customJourneyService.getActivities(groupId, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response));
    }

    @Operation(summary = "Thêm hoạt động mới vào thời khóa biểu hành trình (chỉ Leader khi chưa khóa)")
    @PostMapping("/activities")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<CustomJourneyActivityResponse>> createActivity(
            @Parameter(description = "ID nhóm ghép") @PathVariable UUID groupId,
            @Valid @RequestBody CustomJourneyActivityCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        CustomJourneyActivityResponse response = customJourneyService.createActivity(
                groupId, request, userDetails.getUser().getUserId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED, response, MessageConstant.ACTIVITY_CREATED_SUCCESS));
    }

    @Operation(summary = "Cập nhật hoạt động trong thời khóa biểu (chỉ Leader khi chưa khóa)")
    @PutMapping("/activities/{activityId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<CustomJourneyActivityResponse>> updateActivity(
            @Parameter(description = "ID nhóm ghép") @PathVariable UUID groupId,
            @Parameter(description = "ID hoạt động") @PathVariable UUID activityId,
            @Valid @RequestBody CustomJourneyActivityUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        CustomJourneyActivityResponse response = customJourneyService.updateActivity(
                groupId, activityId, request, userDetails.getUser().getUserId());
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response, MessageConstant.ACTIVITY_UPDATED_SUCCESS));
    }

    @Operation(summary = "Xoá hoạt động khỏi thời khóa biểu (chỉ Leader khi chưa khóa)")
    @DeleteMapping("/activities/{activityId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> deleteActivity(
            @Parameter(description = "ID nhóm ghép") @PathVariable UUID groupId,
            @Parameter(description = "ID hoạt động") @PathVariable UUID activityId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        customJourneyService.deleteActivity(groupId, activityId, userDetails.getUser().getUserId());
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, null, MessageConstant.ACTIVITY_DELETED_SUCCESS));
    }

    @Operation(summary = "Lấy tổng quan dự toán chi phí và danh sách khoản chi của hành trình")
    @GetMapping("/cost-items/summary")
    public ResponseEntity<ApiResponse<com.sep.treksphere.matching.dto.response.CustomJourneyCostSummaryResponse>> getCostSummary(
            @Parameter(description = "ID nhóm ghép") @PathVariable UUID groupId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        UUID currentUserId = userDetails != null ? userDetails.getUser().getUserId() : null;
        com.sep.treksphere.matching.dto.response.CustomJourneyCostSummaryResponse response =
                customJourneyService.getCostSummary(groupId, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response));
    }

    @Operation(summary = "Lấy danh sách các khoản chi dự kiến của hành trình")
    @GetMapping("/cost-items")
    public ResponseEntity<ApiResponse<List<com.sep.treksphere.matching.dto.response.CustomJourneyCostItemResponse>>> getCostItems(
            @Parameter(description = "ID nhóm ghép") @PathVariable UUID groupId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        UUID currentUserId = userDetails != null ? userDetails.getUser().getUserId() : null;
        List<com.sep.treksphere.matching.dto.response.CustomJourneyCostItemResponse> response =
                customJourneyService.getCostItems(groupId, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response));
    }

    @Operation(summary = "Thêm khoản chi dự kiến mới vào hành trình (chỉ Leader khi chưa khóa)")
    @PostMapping("/cost-items")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<com.sep.treksphere.matching.dto.response.CustomJourneyCostItemResponse>> createCostItem(
            @Parameter(description = "ID nhóm ghép") @PathVariable UUID groupId,
            @Valid @RequestBody com.sep.treksphere.matching.dto.request.CustomJourneyCostItemCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        com.sep.treksphere.matching.dto.response.CustomJourneyCostItemResponse response =
                customJourneyService.createCostItem(groupId, request, userDetails.getUser().getUserId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED, response, MessageConstant.COST_ITEM_CREATED_SUCCESS));
    }

    @Operation(summary = "Cập nhật khoản chi dự kiến trong hành trình (chỉ Leader khi chưa khóa)")
    @PutMapping("/cost-items/{costItemId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<com.sep.treksphere.matching.dto.response.CustomJourneyCostItemResponse>> updateCostItem(
            @Parameter(description = "ID nhóm ghép") @PathVariable UUID groupId,
            @Parameter(description = "ID khoản chi dự toán") @PathVariable UUID costItemId,
            @Valid @RequestBody com.sep.treksphere.matching.dto.request.CustomJourneyCostItemUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        com.sep.treksphere.matching.dto.response.CustomJourneyCostItemResponse response =
                customJourneyService.updateCostItem(groupId, costItemId, request, userDetails.getUser().getUserId());
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response, MessageConstant.COST_ITEM_UPDATED_SUCCESS));
    }

    @Operation(summary = "Xoá khoản chi dự kiến khỏi hành trình (chỉ Leader khi chưa khóa)")
    @DeleteMapping("/cost-items/{costItemId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> deleteCostItem(
            @Parameter(description = "ID nhóm ghép") @PathVariable UUID groupId,
            @Parameter(description = "ID khoản chi dự toán") @PathVariable UUID costItemId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        customJourneyService.deleteCostItem(groupId, costItemId, userDetails.getUser().getUserId());
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, null, MessageConstant.COST_ITEM_DELETED_SUCCESS));
    }
}
