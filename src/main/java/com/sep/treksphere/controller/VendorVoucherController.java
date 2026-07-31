package com.sep.treksphere.controller;

import com.sep.treksphere.dto.response.ApiResponse;
import com.sep.treksphere.dto.response.PaginationResponse;
import com.sep.treksphere.dto.response.VoucherResponse;
import com.sep.treksphere.security.CustomUserDetails;
import com.sep.treksphere.service.VoucherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import com.sep.treksphere.utils.PaginationUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;

import com.sep.treksphere.constant.MessageConstant;
import com.sep.treksphere.dto.request.CreateVoucherRequest;
import com.sep.treksphere.dto.request.UpdateVoucherRequest;
import com.sep.treksphere.dto.request.VoucherFilterRequest;
import org.springdoc.core.annotations.ParameterObject;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/vendor/vouchers")
@RequiredArgsConstructor
@Tag(name = "Vendor Voucher", description = "Voucher management APIs for vendor staff and manager")
public class VendorVoucherController {

    private final VoucherService voucherService;

    @GetMapping
    @PreAuthorize("hasAnyRole('VENDOR_MANAGER', 'VENDOR_STAFF')")
    @Operation(summary = "Get list of vouchers for the current vendor")
    public ResponseEntity<ApiResponse<PaginationResponse<VoucherResponse>>> getVendorVouchers(
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @ParameterObject @ModelAttribute VoucherFilterRequest request) {

        Page<VoucherResponse> voucherPage = voucherService.getVouchersForVendorUser(user.getUser().getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, PaginationUtils.toPaginationResponse(voucherPage)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('VENDOR_MANAGER', 'VENDOR_STAFF')")
    @Operation(summary = "Create a new voucher for the current vendor")
    public ResponseEntity<ApiResponse<VoucherResponse>> createVoucher(
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody CreateVoucherRequest request) {

        VoucherResponse response = voucherService.createVoucher(user.getUser().getUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED, response, MessageConstant.VOUCHER_CREATED_SUCCESSFULLY));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('VENDOR_MANAGER', 'VENDOR_STAFF')")
    @Operation(summary = "Update an existing voucher (except code)")
    public ResponseEntity<ApiResponse<VoucherResponse>> updateVoucher(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody UpdateVoucherRequest request) {

        VoucherResponse response = voucherService.updateVoucher(user.getUser().getUserId(), id, request);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response, MessageConstant.VOUCHER_UPDATED_SUCCESSFULLY));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('VENDOR_MANAGER', 'VENDOR_STAFF')")
    @Operation(summary = "Deactivate/delete a voucher")
    public ResponseEntity<ApiResponse<Void>> deleteVoucher(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails user) {

        voucherService.deleteVoucher(user.getUser().getUserId(), id);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, null, MessageConstant.VOUCHER_DELETED_SUCCESSFULLY));
    }
}
