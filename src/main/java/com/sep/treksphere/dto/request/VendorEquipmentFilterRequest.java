package com.sep.treksphere.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Request tìm kiếm thiết bị")
public class VendorEquipmentFilterRequest extends BaseFilterRequest {
}
