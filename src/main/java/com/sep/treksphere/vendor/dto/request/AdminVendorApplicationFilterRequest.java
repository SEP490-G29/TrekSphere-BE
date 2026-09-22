package com.sep.treksphere.vendor.dto.request;

import com.sep.treksphere.common.dto.BaseFilterRequest;
import com.sep.treksphere.vendor.enums.ApplicationStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminVendorApplicationFilterRequest extends BaseFilterRequest {

    private ApplicationStatus status;
}
