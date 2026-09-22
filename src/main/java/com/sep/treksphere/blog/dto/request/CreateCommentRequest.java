package com.sep.treksphere.blog.comment;

import com.sep.treksphere.common.constant.MessageConstant;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateCommentRequest {

    @NotBlank(message = MessageConstant.COMMENT_CONTENT_REQUIRED)
    @Size(max = 1000, message = MessageConstant.COMMENT_CONTENT_MAX_LENGTH)
    private String content;

    private UUID parentCommentId;
}
