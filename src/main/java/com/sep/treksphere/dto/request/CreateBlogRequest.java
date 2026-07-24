package com.sep.treksphere.dto.request;

import com.sep.treksphere.constant.MessageConstant;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateBlogRequest {

    @NotBlank(message = MessageConstant.BLOG_TITLE_REQUIRED)
    private String title;

    @NotBlank(message = MessageConstant.BLOG_CONTENT_REQUIRED)
    private String content;

    private String coverImageUrl;
}
