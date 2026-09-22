package com.sep.treksphere.vendor.application.dto.request;

import com.sep.treksphere.common.dto.BaseFilterRequest;
import com.sep.treksphere.vendor.application.ApplicationStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VendorApplicationFilterRequest extends BaseFilterRequest {
    private ApplicationStatus status;
}
