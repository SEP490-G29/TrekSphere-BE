package com.sep.treksphere.matching.dto.request;

import com.sep.treksphere.matching.enums.CheckpointProgressAction;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request cập nhật tiến độ checkpoint (leader đánh dấu đã đến hoặc bỏ qua)")
public class UpdateCheckpointProgressRequest {

    @NotNull
    @Schema(description = "Hành động leader chọn cho checkpoint này", example = "CHECKED_IN")
    private CheckpointProgressAction status;
}
