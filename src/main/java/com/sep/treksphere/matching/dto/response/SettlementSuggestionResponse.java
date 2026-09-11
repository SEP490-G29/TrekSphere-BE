package com.sep.treksphere.matching.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettlementSuggestionResponse {
    private GroupMemberSummaryResponse fromMember;
    private GroupMemberSummaryResponse toMember;
    private BigDecimal amount;
}
