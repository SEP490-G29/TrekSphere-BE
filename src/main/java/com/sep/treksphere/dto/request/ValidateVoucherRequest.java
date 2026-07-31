package com.sep.treksphere.dto.request;

import com.sep.treksphere.constant.MessageConstant;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidateVoucherRequest {

    @NotBlank(message = MessageConstant.VOUCHER_CODE_REQUIRED)
    @Schema(description = "Mã giảm giá", example = "SUMMER2026")
    private String code;

    @NotNull(message = "Giá trị đơn hàng không được để trống")
    @Min(value = 0, message = MessageConstant.VOUCHER_MIN_ORDER_VALUE_MIN)
    @Schema(description = "Tổng giá trị đơn hàng hiện tại (chưa giảm)", example = "1000000")
    private BigDecimal orderValue;

    @Schema(description = "ID của nhà cung cấp tour (để đối chiếu)", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private UUID vendorId;
}
