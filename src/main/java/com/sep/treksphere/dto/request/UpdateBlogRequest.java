package com.sep.treksphere.dto.request;

import com.sep.treksphere.constant.MessageConstant;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateBlogRequest {

    @Size(max = 500, message = MessageConstant.BLOG_TITLE_MAX_LENGTH)
    private String title;

    private String content;
}
