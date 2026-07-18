package com.sep.treksphere.controller;

import com.sep.treksphere.constant.MessageConstant;
import com.sep.treksphere.dto.request.BaseFilterRequest;
import com.sep.treksphere.dto.request.VendorProfileUpdateRequest;
import com.sep.treksphere.dto.request.VendorStatusUpdateRequest;
import com.sep.treksphere.dto.response.ApiResponse;
import com.sep.treksphere.dto.response.PaginationResponse;
import com.sep.treksphere.dto.response.VendorProfileResponse;
import com.sep.treksphere.dto.response.VendorResponse;
import com.sep.treksphere.security.CustomUserDetails;
import com.sep.treksphere.service.VendorService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.UUID;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/vendors")
@RequiredArgsConstructor
@Tag(name = "Vendor", description = "Các API liên quan đến quản lý Vendor")
public class VendorController {

    private final VendorService vendorService;

    @Operation(summary = "Lấy danh sách Vendor", description = "Trả về danh sách các Vendor (Công ty đối tác) trong hệ thống (Dành cho Admin)")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<ApiResponse<PaginationResponse<VendorResponse>>> getVendors(
            @Valid @ModelAttribute BaseFilterRequest request) {
        
        PaginationResponse<VendorResponse> response = vendorService.getVendors(request);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response));
    }

    @Operation(summary = "Xem hồ sơ Vendor hiện tại", description = "Trả về thông tin hồ sơ chi tiết của Vendor (Dành cho Vendor Manager hoặc Nhân viên thuộc Vendor)")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('VENDOR_MANAGER', 'VENDOR_STAFF')")
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<VendorProfileResponse>> getVendorProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        VendorProfileResponse data = vendorService.getVendorProfile(userDetails);
        
        ApiResponse<VendorProfileResponse> response = ApiResponse.success(
                HttpStatus.OK,
                data,
                MessageConstant.VENDOR_PROFILE_FETCHED
        );
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Cập nhật hồ sơ Vendor hiện tại", description = "Cho phép Vendor Manager cập nhật thông tin chi tiết của doanh nghiệp.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('VENDOR_MANAGER')")
    @PutMapping(value = "/profile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<VendorProfileResponse>> updateVendorProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @ModelAttribute VendorProfileUpdateRequest request) {
        
        VendorProfileResponse data = vendorService.updateVendorProfile(userDetails, request);
        
        ApiResponse<VendorProfileResponse> response = ApiResponse.success(
                HttpStatus.OK,
                data,
                MessageConstant.VENDOR_PROFILE_UPDATED
        );
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Admin thay đổi trạng thái Vendor", description = "Cho phép Admin thay đổi trạng thái hoạt động của đối tác thành ACTIVE, INACTIVE hoặc REVOKED.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{vendorId}/status")
    public ResponseEntity<ApiResponse<VendorResponse>> updateVendorStatus(
            @PathVariable("vendorId") UUID vendorId,
            @Valid @RequestBody VendorStatusUpdateRequest request) {
        
        VendorResponse data = vendorService.updateVendorStatus(vendorId, request);
        
        ApiResponse<VendorResponse> response = ApiResponse.success(
                HttpStatus.OK,
                data,
                MessageConstant.VENDOR_STATUS_UPDATED
        );
        return ResponseEntity.ok(response);
    }
}
