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

    @Schema(description = "Tên ngân hàng nhận hoàn tiền", example = "Vietcombank")
    private String refundBankName;

    @Schema(description = "Số tài khoản nhận hoàn tiền", example = "1234567890")
    private String refundAccountNumber;

    @Schema(description = "Tên chủ tài khoản nhận hoàn tiền", example = "NGUYEN VAN A")
    private String refundAccountHolder;
}
