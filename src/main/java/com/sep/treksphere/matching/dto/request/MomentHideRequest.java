package com.sep.treksphere.matching.dto.request;

import com.sep.treksphere.common.constant.MessageConstant;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request ẩn khoảnh khắc do vi phạm")
public class MomentHideRequest {

    @NotBlank(message = MessageConstant.MOMENT_HIDE_REASON_REQUIRED)
    @Size(max = 1000, message = MessageConstant.MOMENT_HIDE_REASON_MAX_LENGTH)
    @Schema(description = "Lý do ẩn khoảnh khắc", example = "Hình ảnh có nội dung nhạy cảm vi phạm quy định cộng đồng")
    private String hiddenReason;
}
