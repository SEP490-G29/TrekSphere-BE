package com.sep.treksphere.matching.dto.response;

import com.sep.treksphere.matching.enums.SettlementStatus;
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
public class GroupSettlementResponse {
    private UUID groupSettlementId;
    private UUID groupTripId;
    private GroupMemberSummaryResponse fromMember;
    private GroupMemberSummaryResponse toMember;
    private BigDecimal amount;
    private SettlementStatus status;
    private String proofUrl;
    private LocalDateTime submittedAt;
    private LocalDateTime confirmedAt;
    private GroupMemberSummaryResponse confirmedBy;
    private String rejectReason;
    private LocalDateTime createdAt;
}
