package com.sep.treksphere.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BlogCommentFilterRequest extends BaseFilterRequest {

    @Schema(description = "Lọc theo bình luận gốc hay reply (true = chỉ lấy top-level)")
    private Boolean topLevelOnly;
}
