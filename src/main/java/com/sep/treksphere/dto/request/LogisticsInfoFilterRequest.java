package com.sep.treksphere.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LogisticsInfoFilterRequest extends BaseFilterRequest {
    @Schema(description = "Lọc theo trạng thái điểm danh bắt đầu", nullable = true)
    private Boolean isPresentStart;

    @Schema(description = "Lọc theo trạng thái điểm danh kết thúc", nullable = true)
    private Boolean isPresentEnd;
}
