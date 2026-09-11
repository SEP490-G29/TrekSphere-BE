package com.sep.treksphere.matching.dto.request;

import com.sep.treksphere.matching.enums.CostItemCategory;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomJourneyCostItemUpdateRequest {

    @Size(max = 200, message = "Tên khoản chi không được vượt quá 200 ký tự")
    private String itemName;

    private CostItemCategory category;

    @DecimalMin(value = "0.0", message = "Số tiền dự kiến phải lớn hơn hoặc bằng 0")
    private BigDecimal estimatedAmount;

    @Size(max = 1000, message = "Ghi chú không được vượt quá 1000 ký tự")
    private String note;
}
