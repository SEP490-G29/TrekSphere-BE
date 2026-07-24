package com.sep.treksphere.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BlogFilterRequest extends BaseFilterRequest {

    @Schema(description = "Lọc theo tác giả (userId)")
    private String authorId;
}
