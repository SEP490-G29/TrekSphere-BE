package com.sep.treksphere.matching.dto.request;

import com.sep.treksphere.common.constant.MessageConstant;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request từ chối xác nhận thanh toán quyết toán nợ")
public class GroupSettlementRejectRequest {

    @NotBlank(message = MessageConstant.SETTLEMENT_REJECT_REASON_REQUIRED)
    @Size(max = 500, message = MessageConstant.SETTLEMENT_REJECT_REASON_MAX_LENGTH)
    @Schema(description = "Lý do từ chối xác nhận", example = "Chưa nhận được tiền vào tài khoản ngân hàng")
    private String reason;
}
