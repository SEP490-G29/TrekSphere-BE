package com.sep.treksphere.dto.request;

import com.sep.treksphere.constant.MessageConstant;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class CreateReviewRequest {

    @NotNull(message = MessageConstant.REVIEW_BOOKING_REQUIRED)
    private UUID bookingId;

    @NotNull(message = MessageConstant.REVIEW_RATING_REQUIRED)
    @Min(value = 1, message = MessageConstant.REVIEW_RATING_RANGE)
    @Max(value = 5, message = MessageConstant.REVIEW_RATING_RANGE)
    private Integer rating;

    private String content;
}
