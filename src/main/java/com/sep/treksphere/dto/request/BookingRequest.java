package com.sep.treksphere.dto.request;

import com.sep.treksphere.constant.MessageConstant;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class BookingRequest {

    @NotNull(message = MessageConstant.BOOKING_SCHEDULE_REQUIRED)
    @Schema(description = "UUID của lịch khởi hành", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private UUID scheduleId;

    @Schema(description = "Mã voucher giảm giá (nếu có)", example = "DISCOUNT10")
    private String voucherCode;

    @NotEmpty(message = MessageConstant.BOOKING_PARTICIPANTS_REQUIRED)
    @Valid
    @Schema(description = "Danh sách thông tin các thành viên tham gia tour")
    private List<BookingParticipantRequest> participants;
}
