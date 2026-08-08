package com.sep.treksphere.controller;

import com.sep.treksphere.constant.MessageConstant;
import com.sep.treksphere.dto.request.CancellationPolicyRequest;
import com.sep.treksphere.dto.response.ApiResponse;
import com.sep.treksphere.dto.response.CancellationPolicyResponse;
import com.sep.treksphere.security.CustomUserDetails;
import com.sep.treksphere.service.CancellationPolicyService;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/vendor/cancellation-policies")
@RequiredArgsConstructor
@Tag(name = "Vendor Cancellation Policy Management", description = "Các API dành cho Vendor Manager quản lý chính sách hủy tour và hoàn tiền")
@SecurityRequirement(name = "bearerAuth")
public class VendorCancellationPolicyController {

    private final CancellationPolicyService cancellationPolicyService;

    @Operation(summary = "Lấy danh sách chính sách hủy tour của Vendor", description = "Lấy toàn bộ chính sách hủy tour được cấu hình cho Vendor hiện tại")
    @PreAuthorize("hasAnyRole('VENDOR_MANAGER', 'VENDOR_STAFF')")
    @GetMapping
    public ResponseEntity<ApiResponse<List<CancellationPolicyResponse>>> getVendorPolicies(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        List<CancellationPolicyResponse> result = cancellationPolicyService.getVendorPolicies(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, result));
    }

    @Operation(summary = "Tạo mới chính sách hủy tour", description = "Vendor Manager tạo quy định số ngày hủy trước và phần trăm tiền được hoàn lại")
    @PreAuthorize("hasRole('VENDOR_MANAGER')")
    @PostMapping
    public ResponseEntity<ApiResponse<CancellationPolicyResponse>> createPolicy(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CancellationPolicyRequest request
    ) {
        CancellationPolicyResponse result = cancellationPolicyService.createPolicy(userDetails.getUsername(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED, result, MessageConstant.POLICY_CREATED_SUCCESSFULLY));
    }

    @Operation(summary = "Cập nhật chính sách hủy tour", description = "Vendor Manager chỉnh sửa thông tin điều khoản của một chính sách hủy tour đã có")
    @PreAuthorize("hasRole('VENDOR_MANAGER')")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CancellationPolicyResponse>> updatePolicy(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID id,
            @Valid @RequestBody CancellationPolicyRequest request
    ) {
        CancellationPolicyResponse result = cancellationPolicyService.updatePolicy(userDetails.getUsername(), id, request);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, result, MessageConstant.POLICY_UPDATED_SUCCESSFULLY));
    }

    @Operation(summary = "Xóa chính sách hủy tour", description = "Vendor Manager hủy bỏ một chính sách hủy tour")
    @PreAuthorize("hasRole('VENDOR_MANAGER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePolicy(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID id
    ) {
        cancellationPolicyService.deletePolicy(userDetails.getUsername(), id);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, null, MessageConstant.POLICY_DELETED_SUCCESSFULLY));
    }
}
