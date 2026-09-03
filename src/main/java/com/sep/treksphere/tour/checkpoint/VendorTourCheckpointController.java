package com.sep.treksphere.tour.checkpoint;

import com.sep.treksphere.tour.Tour;
import com.sep.treksphere.vendor.Vendor;
import com.sep.treksphere.common.constant.MessageConstant;
import com.sep.treksphere.tour.checkpoint.TourCheckpointRequest;
import com.sep.treksphere.common.dto.ApiResponse;
import com.sep.treksphere.tour.checkpoint.TourCheckpointResponse;
import com.sep.treksphere.common.security.CustomUserDetails;
import com.sep.treksphere.tour.checkpoint.TourCheckpointService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/vendor/tours")
@RequiredArgsConstructor
@Tag(name = "Tour & Schedule", description = "Các API quản lý trạm dừng của Tour (dành cho Vendor)")
public class VendorTourCheckpointController {

    private final TourCheckpointService tourCheckpointService;

    @Operation(summary = "Thêm trạm dừng", description = "Thiết lập/Thêm các trạm dừng chặn (vị trí Lat, Lng, thứ tự trạm, kèm danh sách ảnh)")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('VENDOR')")
    @PostMapping(value = "/{tourId}/checkpoints", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<TourCheckpointResponse>> createCheckpoint(
            @PathVariable UUID tourId,
            @Valid @ModelAttribute TourCheckpointRequest request,
            @RequestParam(value = "images", required = false) List<MultipartFile> images,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        TourCheckpointResponse response = tourCheckpointService.createCheckpoint(
                tourId, request, images, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED, response, MessageConstant.CHECKPOINT_CREATED_SUCCESSFULLY));
    }

    @Operation(summary = "Sửa trạm dừng", description = "Sửa thông tin trạm dừng (toạ độ, mô tả, thứ tự, kèm danh sách ảnh mới)")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('VENDOR')")
    @PutMapping(value = "/checkpoints/{checkpointId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<TourCheckpointResponse>> updateCheckpoint(
            @PathVariable UUID checkpointId,
            @Valid @ModelAttribute TourCheckpointRequest request,
            @RequestParam(value = "images", required = false) List<MultipartFile> images,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        TourCheckpointResponse response = tourCheckpointService.updateCheckpoint(
                checkpointId, request, images, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response, MessageConstant.CHECKPOINT_UPDATED_SUCCESSFULLY));
    }

    @Operation(summary = "Xoá trạm dừng", description = "Xoá trạm dừng khỏi lộ trình")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('VENDOR')")
    @DeleteMapping("/checkpoints/{checkpointId}")
    public ResponseEntity<ApiResponse<Void>> deleteCheckpoint(
            @PathVariable UUID checkpointId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        tourCheckpointService.deleteCheckpoint(checkpointId, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, MessageConstant.CHECKPOINT_DELETED_SUCCESSFULLY));
    }
}
