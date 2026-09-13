package com.sep.treksphere.matching.dto.request;

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
public class GroupSettlementProofRequest {

    @NotBlank(message = "Đường dẫn ảnh chứng từ chuyển tiền không được để trống")
    @Size(max = 500, message = "Đường dẫn ảnh chứng từ không được vượt quá 500 ký tự")
    private String proofUrl;
}
