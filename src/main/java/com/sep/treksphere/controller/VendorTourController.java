package com.sep.treksphere.controller;

import com.sep.treksphere.constant.MessageConstant;
import com.sep.treksphere.dto.request.BaseFilterRequest;
import com.sep.treksphere.dto.request.CreateTourRequest;
import com.sep.treksphere.dto.request.UpdateTourRequest;
import com.sep.treksphere.dto.response.ApiResponse;
import com.sep.treksphere.dto.response.PaginationResponse;
import com.sep.treksphere.dto.response.TourDetailResponse;
import com.sep.treksphere.dto.response.TourSummaryResponse;
import com.sep.treksphere.security.CustomUserDetails;
import com.sep.treksphere.service.TourService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/vendor/tours")
@RequiredArgsConstructor
@Tag(name = "Vendor Tour Management", description = "Các API quản lý Tour dành cho Vendor Manager và Vendor Staff")
public class VendorTourController {

    private final TourService tourService;

    @PreAuthorize("hasAnyRole('VENDOR_MANAGER', 'VENDOR_STAFF')")
    @GetMapping
    public ResponseEntity<ApiResponse<PaginationResponse<TourSummaryResponse>>> getVendorTours(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @ParameterObject @ModelAttribute BaseFilterRequest request) {

        PaginationResponse<TourSummaryResponse> response = tourService.getVendorTours(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response));
    }

    @PreAuthorize("hasAnyRole('VENDOR_MANAGER', 'VENDOR_STAFF')")
    @PostMapping
    public ResponseEntity<ApiResponse<TourDetailResponse>> createTour(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CreateTourRequest request) {

        TourDetailResponse response = tourService.createTour(userDetails.getUsername(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED, response, MessageConstant.TOUR_CREATED_SUCCESSFULLY));
    }

    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('VENDOR_MANAGER', 'VENDOR_STAFF')")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TourDetailResponse>> updateTour(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID id,@Valid @RequestBody UpdateTourRequest request) {

        TourDetailResponse response = tourService.updateTour(userDetails.getUsername(), id, request);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response, MessageConstant.TOUR_UPDATED_SUCCESSFULLY));
    }

    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('VENDOR_MANAGER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTour(
            @AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable UUID id) {

        tourService.deleteTour(userDetails.getUsername(), id);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, null, MessageConstant.TOUR_DELETED_SUCCESSFULLY));
    }

    @Operation(summary = "Gửi yêu cầu kiểm duyệt Tour", description = "VendorStaff/Manager gửi Tour lên hệ thống cho Manager duyệt. Tour phải ở trạng thái DRAFT hoặc REJECTED.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('VENDOR_MANAGER', 'VENDOR_STAFF')")
    @PostMapping("/{id}/submit-approval")
    public ResponseEntity<ApiResponse<TourDetailResponse>> submitTourForApproval(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID id) {

        TourDetailResponse response = tourService.submitTourForApproval(userDetails.getUsername(), id);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response, MessageConstant.TOUR_SUBMITTED_FOR_APPROVAL));
    }
}
