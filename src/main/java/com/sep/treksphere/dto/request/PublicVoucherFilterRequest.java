package com.sep.treksphere.dto.request;

import com.sep.treksphere.enums.voucher.DiscountType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class PublicVoucherFilterRequest extends BaseFilterRequest {
    
    @Schema(description = "Loại giảm giá (PERCENTAGE/FIXED_AMOUNT)")
    private DiscountType discountType;
}
