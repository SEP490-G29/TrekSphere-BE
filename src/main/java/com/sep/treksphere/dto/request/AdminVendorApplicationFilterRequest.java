package com.sep.treksphere.dto.request;

import com.sep.treksphere.enums.vendor.ApplicationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminVendorApplicationFilterRequest extends BaseFilterRequest {

    private ApplicationStatus status;
}
