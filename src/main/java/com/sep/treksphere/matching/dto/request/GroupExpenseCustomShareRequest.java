package com.sep.treksphere.matching.dto.request;

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
public class GroupExpenseCustomShareRequest {

    @NotNull(message = "ID thành viên nhận phần chia không được để trống")
    private UUID matchingMemberId;

    @NotNull(message = "Số tiền chia không được để trống")
    @DecimalMin(value = "0.0", message = "Số tiền chia không được âm")
    private BigDecimal amount;
}
