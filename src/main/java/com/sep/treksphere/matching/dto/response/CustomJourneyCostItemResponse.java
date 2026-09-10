package com.sep.treksphere.matching.dto.response;

import com.sep.treksphere.matching.enums.CostItemCategory;
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
public class CustomJourneyCostItemResponse {
    private UUID customJourneyCostItemId;
    private String itemName;
    private CostItemCategory category;
    private BigDecimal estimatedAmount;
    private String note;
}
