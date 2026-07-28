package com.sep.treksphere.dto.request;

import com.sep.treksphere.enums.voucher.DiscountType;
import com.sep.treksphere.enums.voucher.VoucherStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Getter
@Setter
public class VoucherFilterRequest extends BaseFilterRequest {

    @Schema(description = "Loại giảm giá (PERCENTAGE/FIXED_AMOUNT)")
    private DiscountType discountType;

    @Schema(description = "Trạng thái voucher (ACTIVE/INACTIVE/CANCELLED)")
    private VoucherStatus status;

    @Schema(description = "Lọc theo ngày hết hạn (yyyy-MM-dd)")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate validUntil;

    @Schema(description = "Số lượng tối đa")
    private Integer maxUsage;
}
