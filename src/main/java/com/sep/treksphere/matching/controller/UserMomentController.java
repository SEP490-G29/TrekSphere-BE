package com.sep.treksphere.matching.controller;

import com.sep.treksphere.common.constant.MessageConstant;
import com.sep.treksphere.common.dto.ApiResponse;
import com.sep.treksphere.common.dto.PaginationResponse;
import com.sep.treksphere.common.security.CustomUserDetails;
import com.sep.treksphere.matching.dto.request.MomentCreateRequest;
import com.sep.treksphere.matching.dto.request.MomentFilterRequest;
import com.sep.treksphere.matching.dto.request.MomentUpdateRequest;
import com.sep.treksphere.matching.dto.request.MomentVisibilityUpdateRequest;
import com.sep.treksphere.matching.dto.response.MomentMapResponse;
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
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User Moments", description = "Quản lý nhật ký khoảnh khắc cá nhân và chia sẻ trang cá nhân")
public class UserMomentController {

    private final MomentService momentService;

    @PostMapping("/me/moments")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Tạo khoảnh khắc cá nhân / solo trekking")
    public ResponseEntity<ApiResponse<MomentResponse>> createPersonalMoment(
            @Valid @RequestBody MomentCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        MomentResponse response = momentService.createPersonalMoment(userDetails.getUser().getUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED, response, MessageConstant.MOMENT_CREATED_SUCCESS));
    }

    @PutMapping("/me/moments/{momentId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Tác giả chỉnh sửa khoảnh khắc cá nhân")
    public ResponseEntity<ApiResponse<MomentResponse>> updatePersonalMoment(
            @PathVariable UUID momentId,
            @Valid @RequestBody MomentUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        MomentResponse response = momentService.updatePersonalMoment(momentId, userDetails.getUser().getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response, MessageConstant.MOMENT_UPDATED_SUCCESS));
    }

    @DeleteMapping("/me/moments/{momentId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Tác giả xóa khoảnh khắc cá nhân")
    public ResponseEntity<ApiResponse<Void>> deletePersonalMoment(
            @PathVariable UUID momentId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        momentService.deletePersonalMoment(momentId, userDetails.getUser().getUserId());
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, null, MessageConstant.MOMENT_DELETED_SUCCESS));
    }

    @PatchMapping("/me/moments/{momentId}/visibility")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Tác giả thay đổi quyền hiển thị khoảnh khắc cá nhân (PUBLIC_PROFILE / ONLY_ME)")
    public ResponseEntity<ApiResponse<MomentResponse>> updatePersonalMomentVisibility(
            @PathVariable UUID momentId,
            @Valid @RequestBody MomentVisibilityUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        MomentResponse response = momentService.updatePersonalMomentVisibility(
                momentId, userDetails.getUser().getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response, MessageConstant.MOMENT_VISIBILITY_UPDATED_SUCCESS));
    }

    @GetMapping("/me/moments")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Tác giả xem toàn bộ nhật ký khoảnh khắc cá nhân của mình")
    public ResponseEntity<ApiResponse<PaginationResponse<MomentResponse>>> getMyMoments(
            @Valid @ParameterObject @ModelAttribute MomentFilterRequest filter,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        PaginationResponse<MomentResponse> response = momentService.getMyMoments(
                userDetails.getUser().getUserId(), filter);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response, MessageConstant.MOMENTS_FETCHED_SUCCESS));
    }

    @GetMapping("/me/moments/map")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Tác giả xem bản đồ điểm ghim khoảnh khắc cá nhân của mình")
    public ResponseEntity<ApiResponse<List<MomentMapResponse>>> getMyMomentsMap(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<MomentMapResponse> response = momentService.getMyMomentsMap(userDetails.getUser().getUserId());
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response, MessageConstant.MOMENTS_MAP_FETCHED_SUCCESS));
    }

    @GetMapping("/{userId}/moments")
    @Operation(summary = "Cộng đồng xem các khoảnh khắc công khai (PUBLIC_PROFILE) trên trang cá nhân của một user")
    public ResponseEntity<ApiResponse<PaginationResponse<MomentResponse>>> getUserPublicMoments(
            @PathVariable UUID userId,
            @Valid @ParameterObject @ModelAttribute MomentFilterRequest filter) {
        PaginationResponse<MomentResponse> response = momentService.getUserPublicMoments(userId, filter);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response, MessageConstant.MOMENTS_FETCHED_SUCCESS));
    }

    @GetMapping("/{userId}/moments/map")
    @Operation(summary = "Cộng đồng xem bản đồ các khoảnh khắc công khai (PUBLIC_PROFILE) của một user")
    public ResponseEntity<ApiResponse<List<MomentMapResponse>>> getUserPublicMomentsMap(
            @PathVariable UUID userId) {
        List<MomentMapResponse> response = momentService.getUserPublicMomentsMap(userId);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response, MessageConstant.MOMENTS_MAP_FETCHED_SUCCESS));
    }

    @GetMapping("/moments/{momentId}")
    @Operation(summary = "Xem chi tiết một khoảnh khắc")
    public ResponseEntity<ApiResponse<MomentResponse>> getMomentDetail(
            @PathVariable UUID momentId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        UUID currentUserId = userDetails != null && userDetails.getUser() != null
                ? userDetails.getUser().getUserId()
                : null;
        MomentResponse response = momentService.getMomentDetail(momentId, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response, MessageConstant.MOMENT_FETCHED_SUCCESS));
    }
}
