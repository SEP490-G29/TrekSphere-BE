package com.sep.treksphere.dto.request.report;

import com.sep.treksphere.dto.request.BaseFilterRequest;
import com.sep.treksphere.enums.report.ReportStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReportFilterRequest extends BaseFilterRequest {
    private ReportStatus status;
}