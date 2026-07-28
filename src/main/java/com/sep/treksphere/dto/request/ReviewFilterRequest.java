package com.sep.treksphere.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReviewFilterRequest extends BaseFilterRequest {

    @Schema(description = "Lọc theo điểm đánh giá (1-5)")
    private Integer rating;
}
