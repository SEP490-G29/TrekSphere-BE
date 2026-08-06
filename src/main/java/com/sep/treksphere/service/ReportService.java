package com.sep.treksphere.service;

import com.sep.treksphere.dto.request.report.CreateReportRequest;

import com.sep.treksphere.dto.request.report.ReportFilterRequest;
import com.sep.treksphere.dto.response.PaginationResponse;
import com.sep.treksphere.dto.response.report.ReportResponse;

import java.util.UUID;

public interface ReportService {
    void createReport(CreateReportRequest request, UUID reporterId);
    
    PaginationResponse<ReportResponse> getReportsForAdmin(ReportFilterRequest filter);
}
