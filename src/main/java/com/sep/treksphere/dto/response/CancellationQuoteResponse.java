package com.sep.treksphere.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class CancellationQuoteResponse {
    private BigDecimal paidAmount;
    private BigDecimal alreadyRefundedOrPendingAmount;
    private BigDecimal refundablePaidAmount;
    private BigDecimal nonRefundableCost;
    private Integer refundPercentage;
    private BigDecimal refundAmount;
    private BigDecimal cancellationFee;
    private Long daysBeforeDeparture;
    private String appliedPolicyDescription;
    private Boolean refundDestinationRequired;
}
