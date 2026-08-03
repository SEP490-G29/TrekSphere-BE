package com.sep.treksphere.dto.request;

import com.sep.treksphere.constant.MessageConstant;
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
