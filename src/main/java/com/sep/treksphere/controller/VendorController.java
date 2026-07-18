package com.sep.treksphere.controller;

import com.sep.treksphere.constant.MessageConstant;
import com.sep.treksphere.dto.request.BaseFilterRequest;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
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
}
