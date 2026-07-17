package com.sep.treksphere.dto.request;

import com.sep.treksphere.enums.vendor.ApplicationStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VendorApplicationFilterRequest extends BaseFilterRequest {
    private ApplicationStatus status;
}
