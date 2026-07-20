package com.sep.treksphere.dto.request;

import com.sep.treksphere.enums.vendor.PorterStatus;
import lombok.Getter;
import lombok.Setter;
import org.springdoc.core.annotations.ParameterObject;

@Getter
@Setter
@ParameterObject
public class PorterProfileFilterRequest extends BaseFilterRequest {
    private PorterStatus status;
}
