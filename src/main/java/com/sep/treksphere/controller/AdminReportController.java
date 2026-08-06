package com.sep.treksphere.controller;

import com.sep.treksphere.dto.request.report.ReportFilterRequest;
import com.sep.treksphere.dto.response.ApiResponse;
import com.sep.treksphere.dto.response.PaginationResponse;
import com.sep.treksphere.dto.response.report.ReportResponse;
import com.sep.treksphere.service.ReportService;
import com.sep.treksphere.constant.MessageConstant;
import org.springframework.http.ResponseEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import java.util.UUID;
import com.sep.treksphere.dto.request.report.ResolveReportRequest;
import com.sep.treksphere.security.CustomUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
@RequestMapping("/admin/reports")
@RequiredArgsConstructor
@Tag(name = "Admin Report Management", description = "Quản lý báo cáo vi phạm dành cho Admin")
public class AdminReportController {

    private final ReportService reportService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lấy danh sách báo cáo vi phạm", description = "Admin xem danh sách các báo cáo, có hỗ trợ lọc theo trạng thái và phân trang.")
    public ResponseEntity<ApiResponse<PaginationResponse<ReportResponse>>> getReportsForAdmin(
            @ParameterObject ReportFilterRequest filter) {
        
        PaginationResponse<ReportResponse> response = reportService.getReportsForAdmin(filter);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response, MessageConstant.REPORTS_FETCHED_SUCCESS));
    }

    @PutMapping("/{reportId}/resolve")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Xử lý báo cáo vi phạm", description = "Admin cập nhật trạng thái báo cáo (HIDE_CONTENT, WARNING hoặc DISMISSED).")
    public ResponseEntity<ApiResponse<Void>> resolveReport(
            @PathVariable UUID reportId,
            @Valid @RequestBody ResolveReportRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        reportService.resolveReport(reportId, request, userDetails.getUser().getUserId());
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, null, MessageConstant.REPORT_RESOLVED_SUCCESS));
    }
}
