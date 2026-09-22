package com.sep.treksphere.vendor.statistics;

import com.sep.treksphere.common.dto.ApiResponse;
import com.sep.treksphere.common.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/vendors/profile/statistics")
@RequiredArgsConstructor
@Tag(name = "Vendor Tour Statistics")
@SecurityRequirement(name = "bearerAuth")
public class VendorTourStatisticsController {

    private final VendorTourStatisticsService statisticsService;

    @Operation(summary = "Xem KPI tổng thể và thống kê theo Tour của Vendor hiện tại")
    @PreAuthorize("hasAuthority('VENDOR_STATISTICS_VIEW')")
    @GetMapping
    public ResponseEntity<ApiResponse<VendorTourStatisticsResponse>> getStatistics(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @ParameterObject @ModelAttribute VendorTourStatisticsFilter filter) {
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK,
                statisticsService.getStatistics(userDetails.getUsername(), filter)));
    }
}
