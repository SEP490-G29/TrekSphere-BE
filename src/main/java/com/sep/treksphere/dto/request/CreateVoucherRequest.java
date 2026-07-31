package com.sep.treksphere.dto.request;

import com.sep.treksphere.enums.voucher.DiscountType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.sep.treksphere.constant.MessageConstant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateVoucherRequest {

    @NotBlank(message = MessageConstant.VOUCHER_CODE_REQUIRED)
    @Schema(description = "Mã giảm giá", example = "SUMMER2024")
    private String code;

    @NotNull(message = MessageConstant.VOUCHER_DISCOUNT_TYPE_REQUIRED)
    @Schema(description = "Loại giảm giá (PERCENTAGE hoặc FIXED_AMOUNT)", example = "PERCENTAGE")
    private DiscountType discountType;

    @NotNull(message = MessageConstant.VOUCHER_DISCOUNT_VALUE_REQUIRED)
    @Min(value = 0, message = MessageConstant.VOUCHER_DISCOUNT_VALUE_MIN)
    @Schema(description = "Giá trị giảm giá", example = "20")
    private BigDecimal discountValue;

    @Min(value = 0, message = MessageConstant.VOUCHER_MIN_ORDER_VALUE_MIN)
    @Schema(description = "Giá trị đơn hàng tối thiểu để áp dụng", example = "500000")
    private BigDecimal minOrderValue;

    @NotNull(message = MessageConstant.VOUCHER_MAX_USAGE_REQUIRED)
    @Min(value = 1, message = MessageConstant.VOUCHER_MAX_USAGE_MIN)
    @Schema(description = "Số lượng voucher tối đa có thể sử dụng", example = "100")
    private Integer maxUsage;

    @NotNull(message = MessageConstant.VOUCHER_VALID_FROM_REQUIRED)
    @FutureOrPresent(message = MessageConstant.VOUCHER_VALID_FROM_FUTURE)
    @Schema(description = "Thời điểm voucher bắt đầu có hiệu lực", example = "2026-07-01T00:00:00")
    private LocalDateTime validFrom;

    @NotNull(message = MessageConstant.VOUCHER_VALID_UNTIL_REQUIRED)
    @FutureOrPresent(message = MessageConstant.VOUCHER_VALID_UNTIL_FUTURE)
    @Schema(description = "Thời điểm voucher hết hạn", example = "2026-07-31T23:59:59")
    private LocalDateTime validUntil;

}
