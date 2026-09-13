package com.sep.treksphere.matching.dto.response;

import com.sep.treksphere.matching.enums.BeneficiaryScope;
import com.sep.treksphere.matching.enums.SplitMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupExpenseResponse {
    private UUID groupExpenseId;
    private UUID groupTripId;
    private GroupMemberSummaryResponse payer;
    private String title;
    private BigDecimal amount;
    private BeneficiaryScope beneficiaryScope;
    private Integer beneficiaryCount;
    private SplitMethod splitMethod;
    private LocalDateTime spentAt;
    private String receiptUrl;
    private String note;
    private List<GroupExpenseShareResponse> shares;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
