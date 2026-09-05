package com.sep.treksphere.tour;

import com.sep.treksphere.common.constant.MessageConstant;
import com.sep.treksphere.common.dto.BaseFilterRequest;
import com.sep.treksphere.tour.dto.request.CreateTourRequest;
import com.sep.treksphere.tour.dto.request.UpdateTourRequest;
import com.sep.treksphere.common.dto.ApiResponse;
import com.sep.treksphere.common.dto.PaginationResponse;
import com.sep.treksphere.tour.dto.response.TourDetailResponse;
import com.sep.treksphere.tour.dto.response.TourSummaryResponse;
import com.sep.treksphere.common.security.CustomUserDetails;
import com.sep.treksphere.tour.TourService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
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
@Tag(name = "Vendor Tour Management", description = "Các API quản lý Tour thuộc Vendor hiện tại")
@SecurityRequirement(name = "bearerAuth")
public class VendorTourController {

    private final TourService tourService;

    @PreAuthorize("hasAuthority('TOUR_MANAGE_OWN')")
    @GetMapping
    public ResponseEntity<ApiResponse<PaginationResponse<TourSummaryResponse>>> getVendorTours(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @ParameterObject @ModelAttribute BaseFilterRequest request) {

        PaginationResponse<TourSummaryResponse> response = tourService.getVendorTours(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response));
    }

    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('TOUR_MANAGE_OWN')")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TourDetailResponse>> getVendorTourById(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID id) {

        TourDetailResponse response = tourService.getVendorTourById(userDetails.getUsername(), id);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response));
    }

    @PreAuthorize("hasAuthority('TOUR_MANAGE_OWN')")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<TourDetailResponse>> createTour(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @ModelAttribute CreateTourRequest request,
            @RequestParam(value = "coverImage", required = false) MultipartFile coverImage,
            @RequestParam(value = "tourImages", required = false) List<MultipartFile> tourImages) {

        TourDetailResponse response = tourService.createTour(userDetails.getUsername(), request, coverImage, tourImages);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED, response, MessageConstant.TOUR_CREATED_SUCCESSFULLY));
    }

    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('TOUR_MANAGE_OWN')")
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<TourDetailResponse>> updateTour(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID id,
            @Valid @ModelAttribute UpdateTourRequest request,
            @RequestParam(value = "coverImage", required = false) MultipartFile coverImage,
            @RequestParam(value = "tourImages", required = false) List<MultipartFile> tourImages) {

        TourDetailResponse response = tourService.updateTour(userDetails.getUsername(), id, request, coverImage, tourImages);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response, MessageConstant.TOUR_UPDATED_SUCCESSFULLY));
    }

    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('TOUR_MANAGE_OWN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTour(
            @AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable UUID id) {

        tourService.deleteTour(userDetails.getUsername(), id);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, null, MessageConstant.TOUR_DELETED_SUCCESSFULLY));
    }

    @Operation(summary = "Công khai Tour", description = "Vendor tự công khai Tour DRAFT sau khi đáp ứng đủ điều kiện.")
    @PreAuthorize("hasAuthority('TOUR_PUBLISH')")
    @PutMapping("/{id}/publish")
    public ResponseEntity<ApiResponse<TourDetailResponse>> publishTour(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID id) {
        TourDetailResponse response = tourService.publishTour(userDetails.getUsername(), id);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response, MessageConstant.TOUR_PUBLISHED_SUCCESSFULLY));
    }

    @Operation(summary = "Ngừng công khai Tour", description = "Vendor đưa Tour PUBLISHED về DRAFT khi không có nhóm ghép đang hoạt động.")
    @PreAuthorize("hasAuthority('TOUR_PUBLISH')")
    @PutMapping("/{id}/unpublish")
    public ResponseEntity<ApiResponse<TourDetailResponse>> unpublishTour(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID id) {
        TourDetailResponse response = tourService.unpublishTour(userDetails.getUsername(), id);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response, MessageConstant.TOUR_UNPUBLISHED_SUCCESSFULLY));
    }

    @Operation(summary = "Khôi phục Tour đã xóa", description = "VendorManager khôi phục Tour đã bị xóa mềm. Tour sẽ được chuyển về trạng thái DRAFT cùng toàn bộ dữ liệu con (checkpoint, schedule, image).")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('TOUR_MANAGE_OWN')")
    @PostMapping("/{id}/restore")
    public ResponseEntity<ApiResponse<TourDetailResponse>> restoreTour(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID id) {

        TourDetailResponse response = tourService.restoreTour(userDetails.getUsername(), id);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response, MessageConstant.TOUR_RESTORED_SUCCESSFULLY));
    }

}
