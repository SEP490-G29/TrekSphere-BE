package com.sep.treksphere.controller;

import com.sep.treksphere.dto.response.ApiResponse;
import com.sep.treksphere.dto.response.PaginationResponse;
import com.sep.treksphere.dto.response.VoucherResponse;
import com.sep.treksphere.service.VoucherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import com.sep.treksphere.utils.PaginationUtils;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ModelAttribute;

import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import com.sep.treksphere.dto.request.PublicVoucherFilterRequest;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/vouchers")
@RequiredArgsConstructor
@Tag(name = "Public Vouchers")
public class PublicVoucherController {

    private final VoucherService voucherService;

    @Operation(summary = "Get active vouchers of a vendor")
    @GetMapping("/vendor/{vendorId}")
    public ResponseEntity<ApiResponse<PaginationResponse<VoucherResponse>>> getVendorVouchers(
            @PathVariable UUID vendorId,
            @Valid @ParameterObject @ModelAttribute PublicVoucherFilterRequest request) {
        Page<VoucherResponse> voucherPage = voucherService.getVendorVouchers(vendorId, request);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, PaginationUtils.toPaginationResponse(voucherPage)));
    }
}
