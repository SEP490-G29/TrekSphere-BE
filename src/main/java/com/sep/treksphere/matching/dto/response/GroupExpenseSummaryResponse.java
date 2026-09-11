package com.sep.treksphere.matching.dto.response;

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
public class GroupExpenseSummaryResponse {
    private UUID matchingGroupId;
    private UUID groupTripId;
    private BigDecimal totalExpenseAmount;
    private Integer totalExpensesCount;
    private Integer activeMemberCount;
    private BigDecimal averageExpensePerMember;
}
