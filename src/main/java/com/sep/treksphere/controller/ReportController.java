package com.sep.treksphere.controller;

import com.sep.treksphere.security.CustomUserDetails;
import com.sep.treksphere.constant.MessageConstant;
import com.sep.treksphere.dto.request.report.CreateReportRequest;
import com.sep.treksphere.dto.response.ApiResponse;
import com.sep.treksphere.service.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.UUID;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
@Tag(name = "Quản lý Báo cáo", description = "Các API dùng để báo cáo nội dung vi phạm (Blog, Comment, Review)")
public class ReportController {

    private final ReportService reportService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Tạo báo cáo vi phạm mới", description = "Gửi một báo cáo vi phạm đối với một mục tiêu cụ thể (BLOG, COMMENT, hoặc REVIEW). Yêu cầu người dùng phải đăng nhập.")
    public ResponseEntity<ApiResponse<Void>> createReport(
            @Valid @RequestBody CreateReportRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        UUID reporterId = userDetails.getUser().getUserId();
        reportService.createReport(request, reporterId);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.<Void>builder()
                        .message(MessageConstant.REPORT_CREATED_SUCCESS)
                        .build()
        );
    }
}