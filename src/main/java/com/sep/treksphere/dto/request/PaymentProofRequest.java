package com.sep.treksphere.dto.request;

import com.sep.treksphere.constant.MessageConstant;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentProofRequest {

    @NotBlank(message = MessageConstant.BOOKING_PROOF_IMAGE_REQUIRED)
    @Schema(description = "URL hình ảnh minh chứng đã chuyển khoản ngân hàng", example = "https://example.com/payment-proofs/booking-123.jpg")
    private String proofImageUrl;
}
