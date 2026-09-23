package com.sep.treksphere.report.dto.request;

import com.sep.treksphere.report.enums.ReportAction;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ResolveReportRequest {

    @NotNull(message = "Hành động xử lý không được để trống")
    private ReportAction action;

    private String resolutionNotes;

    @Min(value = 0, message = "Điểm trừ không được âm")
    @Max(value = 100, message = "Điểm trừ không được vượt quá 100")
    private Integer penaltyTrustScore;
}
