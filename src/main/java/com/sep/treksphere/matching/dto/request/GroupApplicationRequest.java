package com.sep.treksphere.matching.dto.request;

import com.sep.treksphere.common.constant.MessageConstant;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request gửi đơn xin gia nhập nhóm ghép")
public class GroupApplicationRequest {

    @Size(max = 500, message = MessageConstant.APPLICATION_MESSAGE_MAX_LENGTH)
    @Schema(description = "Lời nhắn gửi đến trưởng nhóm", example = "Chào bạn, mình có thể lực tốt và đã có kinh nghiệm trekking nhiều cung.")
    private String message;
}
