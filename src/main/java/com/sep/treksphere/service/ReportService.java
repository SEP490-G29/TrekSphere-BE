package com.sep.treksphere.service;

import com.sep.treksphere.dto.request.report.CreateReportRequest;

import java.util.UUID;

public interface ReportService {
    void createReport(CreateReportRequest request, UUID reporterId);
}
