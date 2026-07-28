package com.sep.treksphere.dto.response;

import com.sep.treksphere.enums.voucher.DiscountType;
import com.sep.treksphere.enums.voucher.VoucherStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoucherResponse {
    private UUID voucherId;
    private String code;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private LocalDateTime validFrom;
    private LocalDateTime validUntil;
    private Integer usedCount;
    private Integer maxUsage;
    private BigDecimal minOrderValue;
    private VoucherStatus status;
}
