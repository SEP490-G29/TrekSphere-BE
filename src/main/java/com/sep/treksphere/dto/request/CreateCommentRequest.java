package com.sep.treksphere.dto.request;

import com.sep.treksphere.constant.MessageConstant;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateCommentRequest {

    @NotBlank(message = MessageConstant.COMMENT_CONTENT_REQUIRED)
    private String content;

    private UUID parentCommentId;
}
