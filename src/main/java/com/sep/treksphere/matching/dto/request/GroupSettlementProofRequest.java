package com.sep.treksphere.matching.dto.request;

import com.sep.treksphere.common.constant.MessageConstant;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request gửi bằng chứng thanh toán quyết toán nợ")
public class GroupSettlementProofRequest {

    @NotBlank(message = MessageConstant.SETTLEMENT_PROOF_IMAGE_REQUIRED)
    @Size(max = 500, message = MessageConstant.SETTLEMENT_PROOF_IMAGE_MAX_LENGTH)
    @Schema(description = "Đường dẫn ảnh chứng từ chuyển tiền", example = "https://res.cloudinary.com/.../proof.jpg")
    private String proofUrl;
}
