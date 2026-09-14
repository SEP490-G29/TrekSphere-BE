package com.sep.treksphere.matching.dto.request;

import com.sep.treksphere.common.constant.MessageConstant;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request tạo bình luận bài đăng nhóm")
public class GroupPostCommentCreateRequest {

    @NotBlank(message = MessageConstant.COMMENT_CONTENT_REQUIRED)
    @Schema(description = "Nội dung bình luận", example = "Mình đã chuẩn bị sẵn lều và túi ngủ rồi nhé!")
    private String content;

    @Schema(description = "Mã bình luận cha (nếu là phản hồi)")
    private UUID replyToCommentId;
}
