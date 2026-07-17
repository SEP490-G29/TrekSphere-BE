package com.sep.treksphere.controller;

import com.sep.treksphere.constant.MessageConstant;
import com.sep.treksphere.dto.request.VendorApplicationRequest;
import com.sep.treksphere.dto.response.ApiResponse;
import com.sep.treksphere.dto.response.VendorApplicationResponse;
import com.sep.treksphere.exception.AppException;
import com.sep.treksphere.exception.ErrorCode;
import com.sep.treksphere.security.CustomUserDetails;
import com.sep.treksphere.service.VendorApplicationService;
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
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/vendors/applications")
@RequiredArgsConstructor
@Tag(name = "Vendor Applications", description = "Các API nộp và duyệt đơn đăng ký làm đối tác Vendor")
public class VendorApplicationController {

    private final VendorApplicationService vendorApplicationService;

    @Operation(summary = "Gửi đơn đăng ký đối tác", description = "Cho phép khách du lịch gửi hồ sơ doanh nghiệp ứng tuyển làm Vendor.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('TREKKER')")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<VendorApplicationResponse>> submitApplication(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @ModelAttribute VendorApplicationRequest request) {

        VendorApplicationResponse data = vendorApplicationService.submitApplication(userDetails.getUsername(), request);
        
        ApiResponse<VendorApplicationResponse> response = ApiResponse.success(
                HttpStatus.CREATED, 
                data, 
                MessageConstant.VENDOR_APPLICATION_SUBMITTED
        );
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
