package com.sep.treksphere.dto.request;

import com.sep.treksphere.constant.MessageConstant;
import com.sep.treksphere.enums.blog.ReviewStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateReviewStatusRequest {

    @NotNull(message = MessageConstant.REVIEW_STATUS_REQUIRED_MSG)
    private ReviewStatus status;
}
