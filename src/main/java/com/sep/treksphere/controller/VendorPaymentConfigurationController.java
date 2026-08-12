package com.sep.treksphere.controller;

import com.sep.treksphere.dto.request.PayOsAccountConfigRequest;
import com.sep.treksphere.dto.request.TourPaymentPolicyRequest;
import com.sep.treksphere.dto.response.ApiResponse;
import com.sep.treksphere.dto.response.TourPaymentPolicyResponse;
import com.sep.treksphere.dto.response.VendorPaymentAccountResponse;
import com.sep.treksphere.security.CustomUserDetails;
import com.sep.treksphere.service.VendorPaymentConfigurationService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/vendor/payment-settings")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Vendor Payment Settings")
@PreAuthorize("hasRole('VENDOR_MANAGER')")
public class VendorPaymentConfigurationController {

    private final VendorPaymentConfigurationService service;

    @PutMapping("/payos-account")
    public ResponseEntity<ApiResponse<VendorPaymentAccountResponse>> configurePayOs(
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody PayOsAccountConfigRequest request) {
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK,
                service.configurePayOsAccount(user.getUsername(), request)));
    }

    @GetMapping("/payos-account")
    public ResponseEntity<ApiResponse<VendorPaymentAccountResponse>> getPayOs(
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK,
                service.getPayOsAccount(user.getUsername())));
    }

    @PutMapping("/tours/{tourId}/policy")
    public ResponseEntity<ApiResponse<TourPaymentPolicyResponse>> updatePolicy(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable UUID tourId,
            @Valid @RequestBody TourPaymentPolicyRequest request) {
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK,
                service.updateTourPaymentPolicy(user.getUsername(), tourId, request)));
    }

    @GetMapping("/tours/{tourId}/policy")
    public ResponseEntity<ApiResponse<TourPaymentPolicyResponse>> getPolicy(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable UUID tourId) {
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK,
                service.getTourPaymentPolicy(user.getUsername(), tourId)));
    }
}
