package com.sep.treksphere.dto.request;

import com.sep.treksphere.constant.MessageConstant;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateBlogRequest {

    @NotBlank(message = MessageConstant.BLOG_TITLE_REQUIRED)
    @Size(max = 500, message = MessageConstant.BLOG_TITLE_MAX_LENGTH)
    private String title;

    @NotBlank(message = MessageConstant.BLOG_CONTENT_REQUIRED)
    private String content;
}
