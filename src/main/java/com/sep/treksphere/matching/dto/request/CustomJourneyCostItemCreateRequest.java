package com.sep.treksphere.matching.dto.request;

import com.sep.treksphere.common.constant.MessageConstant;
import com.sep.treksphere.matching.enums.CostItemCategory;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Request tạo khoản chi phí dự kiến trong hành trình")
public class CustomJourneyCostItemCreateRequest {

    @NotBlank(message = MessageConstant.COST_ITEM_NAME_REQUIRED)
    @Size(max = 200, message = MessageConstant.COST_ITEM_NAME_MAX_LENGTH)
    @Schema(description = "Tên khoản chi", example = "Vé vào cửa vườn quốc gia")
    private String itemName;

    @Schema(description = "Danh mục chi phí (PERMIT, GUIDE, FOOD, TRANSPORT, GEAR, OTHER)", example = "PERMIT")
    private CostItemCategory category;

    @NotNull(message = MessageConstant.COST_ITEM_AMOUNT_REQUIRED)
    @DecimalMin(value = "0.0", message = MessageConstant.COST_ITEM_AMOUNT_MIN)
    @Schema(description = "Số tiền dự kiến", example = "80000")
    private BigDecimal estimatedAmount;

    @Size(max = 1000, message = MessageConstant.COST_ITEM_NOTE_MAX_LENGTH)
    @Schema(description = "Ghi chú chi tiết khoản chi", example = "Mua trực tiếp tại cổng ban quản lý")
    private String note;
}
