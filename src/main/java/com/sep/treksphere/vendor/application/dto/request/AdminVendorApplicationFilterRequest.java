package com.sep.treksphere.vendor.application.dto.request;

import com.sep.treksphere.common.dto.BaseFilterRequest;
import com.sep.treksphere.vendor.application.ApplicationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminVendorApplicationFilterRequest extends BaseFilterRequest {

    private ApplicationStatus status;
}
