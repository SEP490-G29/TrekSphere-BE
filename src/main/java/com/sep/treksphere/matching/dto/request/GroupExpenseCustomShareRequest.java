package com.sep.treksphere.matching.dto.request;

import com.sep.treksphere.common.constant.MessageConstant;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
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
@Schema(description = "Request chia tiền tùy chỉnh cho từng thành viên")
public class GroupExpenseCustomShareRequest {

    @NotNull(message = MessageConstant.EXPENSE_SHARE_MEMBER_REQUIRED)
    @Schema(description = "ID thành viên (MatchingMember) nhận phần chi tiêu")
    private UUID matchingMemberId;

    @NotNull(message = MessageConstant.EXPENSE_SHARE_AMOUNT_REQUIRED)
    @DecimalMin(value = "0.0", message = MessageConstant.EXPENSE_SHARE_AMOUNT_MIN)
    @Schema(description = "Số tiền hoặc tỷ lệ phân bổ", example = "150000")
    private BigDecimal amount;
}
