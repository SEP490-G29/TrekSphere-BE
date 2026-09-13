package com.sep.treksphere.matching.dto.request;

import com.sep.treksphere.common.dto.BaseFilterRequest;
import com.sep.treksphere.matching.enums.BeneficiaryScope;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class GroupExpenseFilterRequest extends BaseFilterRequest {

    @Schema(description = "Lọc theo thành viên đã chi trả tiền")
    private UUID paidByMemberId;

    @Schema(description = "Lọc theo phạm vi thụ hưởng (ALL_MEMBERS hoặc SELECTED_MEMBERS)")
    private BeneficiaryScope beneficiaryScope;
}
