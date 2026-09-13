package com.sep.treksphere.report;

import com.sep.treksphere.common.dto.BaseFilterRequest;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReportFilterRequest extends BaseFilterRequest {
    private ReportStatus status;
}