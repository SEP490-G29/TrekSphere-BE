package com.sep.treksphere.controller;

import com.sep.treksphere.constant.MessageConstant;
import com.sep.treksphere.dto.request.VendorApplicationFilterRequest;
import com.sep.treksphere.dto.request.VendorApplicationReviewRequest;
import com.sep.treksphere.dto.request.VendorApplicationRequest;
import com.sep.treksphere.dto.request.VendorApplicationUpdateRequest;
import com.sep.treksphere.dto.response.ApiResponse;
import com.sep.treksphere.dto.response.PaginationResponse;
import com.sep.treksphere.dto.response.VendorApplicationResponse;
import com.sep.treksphere.security.CustomUserDetails;
import com.sep.treksphere.service.VendorApplicationService;
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

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/vendors/applications")
@RequiredArgsConstructor
@Tag(name = "Vendor Applications", description = "Các API nộp và duyệt đơn đăng ký làm đối tác Vendor")
public class VendorApplicationController {

    private final VendorApplicationService vendorApplicationService;

    @Operation(summary = "Tạo đơn đăng ký bản nháp", description = "Cho phép khách du lịch khởi tạo đơn đăng ký Vendor dưới dạng bản nháp.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('TREKKER')")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<VendorApplicationResponse>> saveDraftApplication(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @ModelAttribute VendorApplicationRequest request) {

        VendorApplicationResponse data = vendorApplicationService.saveDraftApplication(userDetails.getUser().getUserId(), request);

        ApiResponse<VendorApplicationResponse> response = ApiResponse.success(
                HttpStatus.CREATED,
                data,
                MessageConstant.VENDOR_APPLICATION_DRAFT_CREATED
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Lấy danh sách đơn đăng ký (Admin)", description = "Admin lọc và phân trang toàn bộ các đơn đăng ký đối tác.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<ApiResponse<PaginationResponse<VendorApplicationResponse>>> getApplications(
            @Valid @ParameterObject @ModelAttribute VendorApplicationFilterRequest request) {

        PaginationResponse<VendorApplicationResponse> data = vendorApplicationService.getApplications(request);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, data));
    }

    @Operation(summary = "Xem chi tiết đơn đăng ký", description = "Admin hoặc Trekker nộp đơn có thể xem chi tiết hồ sơ đơn ứng tuyển.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<VendorApplicationResponse>> getApplicationById(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        VendorApplicationResponse data = vendorApplicationService.getApplicationById(id, userDetails);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, data));
    }

    @Operation(summary = "Kiểm duyệt đơn đăng ký (Admin)", description = "Admin phê duyệt (APPROVED) hoặc từ chối (REJECTED) đơn ứng tuyển của đối tác.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/review")
    public ResponseEntity<ApiResponse<VendorApplicationResponse>> reviewApplication(
            @PathVariable UUID id,
            @Valid @RequestBody VendorApplicationReviewRequest request) {
        
        VendorApplicationResponse data = vendorApplicationService.reviewApplication(id, request);
        
        ApiResponse<VendorApplicationResponse> response = ApiResponse.success(
                HttpStatus.OK, 
                data, 
                MessageConstant.VENDOR_APPLICATION_REVIEWED
        );
        
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Cập nhật nội dung đơn đăng ký", description = "Cho phép Trekker cập nhật lại thông tin hồ sơ của đơn nháp (DRAFT) hoặc đơn bị từ chối (REJECTED).")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('TREKKER')")
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<VendorApplicationResponse>> updateApplication(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @ModelAttribute VendorApplicationUpdateRequest request) {
        
        VendorApplicationResponse data = vendorApplicationService.updateApplication(id, request, userDetails.getUser().getUserId());
        
        ApiResponse<VendorApplicationResponse> response = ApiResponse.success(
                HttpStatus.OK, 
                data, 
                MessageConstant.VENDOR_APPLICATION_UPDATED
        );
        
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Nộp đơn đăng ký bản nháp", description = "Cho phép Trekker nộp đơn đăng ký từ trạng thái bản nháp (DRAFT) lên PENDING để chờ duyệt.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('TREKKER')")
    @PostMapping("/{id}/submit")
    public ResponseEntity<ApiResponse<VendorApplicationResponse>> submitDraftApplication(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        VendorApplicationResponse data = vendorApplicationService.submitDraftApplication(id, userDetails.getUser().getUserId());
        
        ApiResponse<VendorApplicationResponse> response = ApiResponse.success(
                HttpStatus.OK, 
                data, 
                MessageConstant.VENDOR_APPLICATION_SUBMITTED
        );
        
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Nộp lại đơn đăng ký bị từ chối", description = "Cho phép Trekker nộp lại đơn đăng ký từ trạng thái bị từ chối (REJECTED) lên PENDING để chờ duyệt lại.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('TREKKER')")
    @PostMapping("/{id}/resubmit")
    public ResponseEntity<ApiResponse<VendorApplicationResponse>> resubmitRejectedApplication(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        VendorApplicationResponse data = vendorApplicationService.resubmitRejectedApplication(id, userDetails.getUser().getUserId());
        
        ApiResponse<VendorApplicationResponse> response = ApiResponse.success(
                HttpStatus.OK, 
                data, 
                MessageConstant.VENDOR_APPLICATION_RESUBMITTED
        );
        
        return ResponseEntity.ok(response);
    }
}
