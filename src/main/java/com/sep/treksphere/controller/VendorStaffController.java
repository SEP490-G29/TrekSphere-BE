package com.sep.treksphere.controller;

import com.sep.treksphere.dto.request.BaseFilterRequest;
import com.sep.treksphere.dto.response.ApiResponse;
import com.sep.treksphere.dto.response.PaginationResponse;
import com.sep.treksphere.dto.response.VendorStaffResponse;
import com.sep.treksphere.security.CustomUserDetails;
import com.sep.treksphere.service.VendorStaffService;
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
@RequestMapping("/api/v1/vendor-staff")
@RequiredArgsConstructor
@Tag(name = "Vendor Staff", description = "Các API liên quan đến quản lý nhân viên của Vendor")
public class VendorStaffController {

    private final VendorStaffService vendorStaffService;

    @Operation(summary = "Lấy danh sách nhân viên của công ty", description = "Trả về danh sách nhân viên thuộc công ty của Vendor Manager hiện tại")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('VENDOR_MANAGER')")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<PaginationResponse<VendorStaffResponse>>> getMyVendorStaff(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @org.springdoc.core.annotations.ParameterObject @ModelAttribute BaseFilterRequest request) {

        PaginationResponse<VendorStaffResponse> response = vendorStaffService.getMyVendorStaff(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response));
    }
}
