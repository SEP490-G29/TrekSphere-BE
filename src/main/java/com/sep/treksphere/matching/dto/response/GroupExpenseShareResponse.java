package com.sep.treksphere.matching.dto.response;

import com.sep.treksphere.matching.enums.ExpenseShareSettlementStatus;
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
public class GroupExpenseShareResponse {
    private UUID groupExpenseShareId;
    private GroupMemberSummaryResponse member;
    private BigDecimal shareAmount;
    private String reason;
    private ExpenseShareSettlementStatus settlementStatus;
    private LocalDateTime settledAt;
}
