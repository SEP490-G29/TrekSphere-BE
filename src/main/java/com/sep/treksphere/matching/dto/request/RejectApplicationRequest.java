package com.sep.treksphere.matching.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request từ chối đơn xin tham gia nhóm ghép")
public class RejectApplicationRequest {

    @Size(max = 500, message = "Lý do từ chối không được vượt quá 500 ký tự")
    @Schema(description = "Lý do từ chối đơn tham gia (tùy chọn)", example = "Không đủ điều kiện thể lực cho cung trekking này")
    private String rejectReason;
}

