package com.sep.treksphere.dto.request;

import lombok.Data;

@Data
public class UpdateBlogRequest {

    private String title;

    private String content;

    private String coverImageUrl;
}
