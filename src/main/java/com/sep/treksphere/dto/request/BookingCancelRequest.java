package com.sep.treksphere.dto.request;

import com.sep.treksphere.constant.MessageConstant;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BookingCancelRequest {

    @NotBlank(message = MessageConstant.BOOKING_CANCEL_REASON_REQUIRED)
    @Schema(description = "Lý do hủy đơn đặt tour", example = "Thay đổi lịch trình cá nhân")
    private String cancellationReason;
}
