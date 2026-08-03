package com.sep.treksphere.dto.response;

import com.sep.treksphere.enums.user.Gender;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogisticsPassengerResponse {

    @Schema(description = "ID của hành khách")
    private UUID participantId;

    @Schema(description = "Họ và tên")
    private String fullName;

    @Schema(description = "Giới tính")
    private Gender gender;

    @Schema(description = "Số điện thoại")
    private String phone;

    @Schema(description = "Yêu cầu đặc biệt (thuế, đồ ăn, bệnh lý, ...)")
    private String specialRequirements;

    @Schema(description = "Trạng thái điểm danh lúc bắt đầu")
    private Boolean isPresentStart;

    @Schema(description = "Thời gian điểm danh bắt đầu")
    private LocalDateTime startAttendedAt;

    @Schema(description = "Trạng thái điểm danh lúc kết thúc")
    private Boolean isPresentEnd;

    @Schema(description = "Thời gian điểm danh kết thúc")
    private LocalDateTime endAttendedAt;
}
