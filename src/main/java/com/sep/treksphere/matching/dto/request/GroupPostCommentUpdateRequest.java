package com.sep.treksphere.matching.dto.request;

import com.sep.treksphere.common.constant.MessageConstant;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request cập nhật bình luận bài đăng nhóm")
public class GroupPostCommentUpdateRequest {

    @NotBlank(message = MessageConstant.COMMENT_CONTENT_REQUIRED)
    @Schema(description = "Nội dung bình luận cập nhật", example = "Mình đã chuẩn bị sẵn 2 lều 4 người rồi nhé!")
    private String content;
}
