package com.sep.treksphere.report.dto.request;

import com.sep.treksphere.common.dto.BaseFilterRequest;
import com.sep.treksphere.report.enums.ReportStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReportFilterRequest extends BaseFilterRequest {
    private ReportStatus status;
}