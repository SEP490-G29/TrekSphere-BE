package com.sep.treksphere.dto.request.report;

import com.sep.treksphere.constant.MessageConstant;
import com.sep.treksphere.enums.report.ReportTargetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateReportRequest {

    @NotNull(message = MessageConstant.REPORT_TARGET_TYPE_REQUIRED)
    private ReportTargetType targetType;

    @NotNull(message = MessageConstant.REPORT_TARGET_ID_REQUIRED)
    private UUID targetId;

    @NotBlank(message = MessageConstant.REPORT_REASON_REQUIRED)
    private String reason;

}
