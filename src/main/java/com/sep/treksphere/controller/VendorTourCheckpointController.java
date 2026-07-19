package com.sep.treksphere.controller;

import com.sep.treksphere.constant.MessageConstant;
import com.sep.treksphere.dto.request.TourCheckpointRequest;
import com.sep.treksphere.dto.response.ApiResponse;
import com.sep.treksphere.dto.response.TourCheckpointResponse;
import com.sep.treksphere.security.CustomUserDetails;
import com.sep.treksphere.service.TourCheckpointService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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
@RequestMapping("/api/v1/vendor/tours")
@RequiredArgsConstructor
@Tag(name = "Tour & Schedule", description = "Các API quản lý trạm dừng của Tour (dành cho Vendor)")
public class VendorTourCheckpointController {

    private final TourCheckpointService tourCheckpointService;

    @Operation(summary = "Thêm trạm dừng", description = "Thiết lập/Thêm các trạm dừng chặn (vị trí Lat, Lng, thứ tự trạm)")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('VENDOR_MANAGER', 'VENDOR_STAFF')")
    @PostMapping("/{tourId}/checkpoints")
    public ResponseEntity<ApiResponse<TourCheckpointResponse>> createCheckpoint(
            @PathVariable UUID tourId,
            @Valid @RequestBody TourCheckpointRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        TourCheckpointResponse response = tourCheckpointService.createCheckpoint(
                tourId, request, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED, response, MessageConstant.CHECKPOINT_CREATED_SUCCESSFULLY));
    }

    @Operation(summary = "Sửa trạm dừng", description = "Sửa thông tin trạm dừng (toạ độ, mô tả, thứ tự...)")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('VENDOR_MANAGER', 'VENDOR_STAFF')")
    @PutMapping("/checkpoints/{checkpointId}")
    public ResponseEntity<ApiResponse<TourCheckpointResponse>> updateCheckpoint(
            @PathVariable UUID checkpointId,
            @Valid @RequestBody TourCheckpointRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        TourCheckpointResponse response = tourCheckpointService.updateCheckpoint(
                checkpointId, request, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response, MessageConstant.CHECKPOINT_UPDATED_SUCCESSFULLY));
    }

    @Operation(summary = "Xoá trạm dừng", description = "Xoá trạm dừng khỏi lộ trình")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('VENDOR_MANAGER', 'VENDOR_STAFF')")
    @DeleteMapping("/checkpoints/{checkpointId}")
    public ResponseEntity<ApiResponse<Void>> deleteCheckpoint(
            @PathVariable UUID checkpointId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        tourCheckpointService.deleteCheckpoint(checkpointId, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, MessageConstant.CHECKPOINT_DELETED_SUCCESSFULLY));
    }
}
