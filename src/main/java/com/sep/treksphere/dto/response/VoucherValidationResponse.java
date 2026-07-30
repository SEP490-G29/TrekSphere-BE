package com.sep.treksphere.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoucherValidationResponse {

    @Schema(description = "Cờ báo hiệu mã giảm giá có áp dụng được hay không", example = "true")
    private boolean isValid;

    @Schema(description = "Số tiền được giảm (nếu hợp lệ)", example = "200000")
    private BigDecimal discountAmount;

    @Schema(description = "Câu thông báo kết quả", example = "Áp dụng mã giảm giá thành công!")
    private String message;
}
