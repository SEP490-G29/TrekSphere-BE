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
public class MemberBalanceResponse {
    private GroupMemberSummaryResponse member;
    private BigDecimal totalPaid;
    private BigDecimal totalShare;
    private BigDecimal netBalance;
    private String balanceType; // CREDITOR (>0), DEBTOR (<0), BALANCED (==0)
}
