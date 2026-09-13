package com.sep.treksphere.matching.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MomentHideRequest {

    @NotBlank(message = "Vui lòng nhập lý do ẩn khoảnh khắc")
    @Size(max = 1000, message = "Lý do ẩn không được vượt quá 1000 ký tự")
    private String hiddenReason;
}
