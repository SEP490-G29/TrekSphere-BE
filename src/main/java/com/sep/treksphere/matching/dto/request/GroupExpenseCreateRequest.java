package com.sep.treksphere.matching.dto.request;

import com.sep.treksphere.common.constant.MessageConstant;
import com.sep.treksphere.matching.enums.BeneficiaryScope;
import com.sep.treksphere.matching.enums.SplitMethod;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request tạo khoản chi tiêu nhóm")
public class GroupExpenseCreateRequest {

    @NotBlank(message = MessageConstant.EXPENSE_NAME_REQUIRED)
    @Size(max = 200, message = MessageConstant.EXPENSE_NAME_MAX_LENGTH)
    @Schema(description = "Tên khoản chi tiêu", example = "Tiền thuê lều cắm trại")
    private String title;

    @NotNull(message = MessageConstant.EXPENSE_AMOUNT_REQUIRED)
    @DecimalMin(value = "1.0", message = MessageConstant.EXPENSE_AMOUNT_MIN)
    @Schema(description = "Số tiền chi tiêu", example = "500000")
    private BigDecimal amount;

    @Schema(description = "Mã thành viên chi tiền (để trống nếu người tạo tự chi)")
    private UUID paidByMemberId;

    @NotNull(message = MessageConstant.EXPENSE_SPLIT_SCOPE_REQUIRED)
    @Schema(description = "Phạm vi chia tiền (ALL_MEMBERS hoặc CUSTOM)", example = "ALL_MEMBERS")
    private BeneficiaryScope beneficiaryScope;

    @Schema(description = "Danh sách ID thành viên thụ hưởng nếu scope là CUSTOM")
    private List<UUID> beneficiaryMemberIds;

    @Builder.Default
    @Schema(description = "Phương thức chia (EQUAL, PERCENTAGE, EXACT_AMOUNT)", example = "EQUAL")
    private SplitMethod splitMethod = SplitMethod.EQUAL;

    @Schema(description = "Chi tiết phân bổ số tiền nếu chia tùy chỉnh")
    private List<GroupExpenseCustomShareRequest> customShares;

    @Schema(description = "Thời điểm phát sinh chi tiêu")
    private LocalDateTime spentAt;

    @Size(max = 500, message = MessageConstant.EXPENSE_PROOF_IMAGE_MAX_LENGTH)
    @Schema(description = "Đường dẫn ảnh hóa đơn/biên nhận", example = "https://res.cloudinary.com/.../receipt.jpg")
    private String receiptUrl;

    @Schema(description = "Ghi chú thêm về khoản chi", example = "Thuê 2 lều 4 người tại trạm kiểm lâm")
    private String note;
}
