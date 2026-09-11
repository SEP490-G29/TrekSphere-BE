package com.sep.treksphere.matching.dto.request;

import com.sep.treksphere.matching.enums.CostItemCategory;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class CustomJourneyCostItemCreateRequest {

    @NotBlank(message = "Tên khoản chi không được để trống")
    @Size(max = 200, message = "Tên khoản chi không được vượt quá 200 ký tự")
    private String itemName;

    private CostItemCategory category;

    @NotNull(message = "Số tiền dự kiến không được để trống")
    @DecimalMin(value = "0.0", message = "Số tiền dự kiến phải lớn hơn hoặc bằng 0")
    private BigDecimal estimatedAmount;

    @Size(max = 1000, message = "Ghi chú không được vượt quá 1000 ký tự")
    private String note;
}
