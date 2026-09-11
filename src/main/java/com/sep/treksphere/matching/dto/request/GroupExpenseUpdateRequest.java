package com.sep.treksphere.matching.dto.request;

import com.sep.treksphere.matching.enums.BeneficiaryScope;
import com.sep.treksphere.matching.enums.SplitMethod;
import jakarta.validation.constraints.DecimalMin;
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
public class GroupExpenseUpdateRequest {

    @Size(max = 200, message = "Tên khoản chi tiêu không được vượt quá 200 ký tự")
    private String title;

    @DecimalMin(value = "1.0", message = "Số tiền chi tiêu phải lớn hơn 0")
    private BigDecimal amount;

    private UUID paidByMemberId;

    private BeneficiaryScope beneficiaryScope;

    private List<UUID> beneficiaryMemberIds;

    private SplitMethod splitMethod;

    private LocalDateTime spentAt;

    @Size(max = 500, message = "Đường dẫn hóa đơn không được vượt quá 500 ký tự")
    private String receiptUrl;

    private String note;
}
