package com.sep.treksphere.dto.request;

import com.sep.treksphere.enums.booking.RefundReason;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VendorBookingCancelRequest {
    @NotNull
    private RefundReason reason;

    @NotBlank
    private String reasonDetail;
}
