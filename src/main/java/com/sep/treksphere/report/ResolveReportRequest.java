package com.sep.treksphere.report;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ResolveReportRequest {

    @NotNull(message = "Hành động xử lý không được để trống")
    private ReportAction action;

    private String resolutionNotes;
}
