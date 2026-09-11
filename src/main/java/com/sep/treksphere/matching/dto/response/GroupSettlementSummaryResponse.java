package com.sep.treksphere.matching.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupSettlementSummaryResponse {
    private UUID matchingGroupId;
    private UUID groupTripId;
    private BigDecimal totalGroupExpense;
    private List<MemberBalanceResponse> memberBalances;
    private List<SettlementSuggestionResponse> suggestions;
    private boolean isFullySettled;
}
