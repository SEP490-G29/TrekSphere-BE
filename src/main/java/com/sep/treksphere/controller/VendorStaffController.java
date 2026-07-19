package com.sep.treksphere.controller;

import com.sep.treksphere.constant.MessageConstant;
import com.sep.treksphere.dto.request.BaseFilterRequest;
import com.sep.treksphere.dto.request.VendorStaffAddRequest;
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
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
            @Valid @ParameterObject @ModelAttribute BaseFilterRequest request) {

        PaginationResponse<VendorStaffResponse> response = vendorStaffService.getMyVendorStaff(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response));
    }

    @Operation(summary = "Thêm nhân viên mới vào Vendor", description = "Cho phép Vendor Manager thêm nhân viên mới bằng cách gán User có sẵn hoặc tạo User mới và gửi email kích hoạt.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('VENDOR_MANAGER')")
    @PostMapping
    public ResponseEntity<ApiResponse<VendorStaffResponse>> addVendorStaff(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody VendorStaffAddRequest request) {
        
        VendorStaffResponse data = vendorStaffService.addVendorStaff(userDetails.getUsername(), request);
        
        ApiResponse<VendorStaffResponse> response = ApiResponse.success(
                HttpStatus.CREATED,
                data,
                MessageConstant.VENDOR_STAFF_ADDED
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
