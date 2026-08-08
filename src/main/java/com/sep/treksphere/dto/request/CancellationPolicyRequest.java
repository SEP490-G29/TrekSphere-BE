package com.sep.treksphere.dto.request;

import com.sep.treksphere.constant.MessageConstant;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CancellationPolicyRequest {

    @NotNull(message = MessageConstant.POLICY_CANCEL_DAYS_REQUIRED)
    @Min(value = 0, message = MessageConstant.POLICY_CANCEL_DAYS_MIN)
    @Schema(description = "Số ngày hủy trước thời điểm khởi hành", example = "7")
    private Integer cancelBeforeDays;

    @NotNull(message = MessageConstant.POLICY_REFUND_PERCENTAGE_REQUIRED)
    @Min(value = 0, message = MessageConstant.POLICY_REFUND_PERCENTAGE_RANGE)
    @Max(value = 100, message = MessageConstant.POLICY_REFUND_PERCENTAGE_RANGE)
    @Schema(description = "Phần trăm tiền được hoàn lại (0 - 100%)", example = "80")
    private Integer refundPercentage;

    @Schema(description = "Mô tả chi tiết điều khoản hủy", example = "Hủy trước 7 ngày được hoàn 80% giá trị đơn hàng")
    private String description;
}
