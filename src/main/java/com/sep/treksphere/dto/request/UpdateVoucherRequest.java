package com.sep.treksphere.dto.request;

import com.sep.treksphere.constant.MessageConstant;
import com.sep.treksphere.enums.voucher.DiscountType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateVoucherRequest {

    @Schema(description = "Loại giảm giá (PERCENTAGE hoặc FIXED_AMOUNT)", example = "PERCENTAGE")
    private DiscountType discountType;

    @Min(value = 0, message = MessageConstant.VOUCHER_DISCOUNT_VALUE_MIN)
    @Schema(description = "Giá trị giảm giá", example = "20")
    private BigDecimal discountValue;

    @Min(value = 0, message = MessageConstant.VOUCHER_MIN_ORDER_VALUE_MIN)
    @Schema(description = "Giá trị đơn hàng tối thiểu để áp dụng", example = "500000")
    private BigDecimal minOrderValue;

    @Min(value = 1, message = MessageConstant.VOUCHER_MAX_USAGE_MIN)
    @Schema(description = "Số lượng voucher tối đa có thể sử dụng", example = "100")
    private Integer maxUsage;

    @Schema(description = "Thời điểm voucher bắt đầu có hiệu lực", example = "2026-07-01T00:00:00")
    private LocalDateTime validFrom;

    @FutureOrPresent(message = MessageConstant.VOUCHER_VALID_UNTIL_FUTURE)
    @Schema(description = "Thời điểm voucher hết hạn", example = "2026-07-31T23:59:59")
    private LocalDateTime validUntil;

    @Schema(description = "Trạng thái của voucher", example = "ACTIVE")
    private com.sep.treksphere.enums.voucher.VoucherStatus status;

}
