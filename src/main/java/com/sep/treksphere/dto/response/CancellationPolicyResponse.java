package com.sep.treksphere.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CancellationPolicyResponse {
    private String cancellationPolicyId;
    private Integer cancelBeforeDays;
    private Integer refundPercentage;
    private String description;
    private Boolean isActive;
}
