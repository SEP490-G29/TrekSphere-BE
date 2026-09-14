package com.sep.treksphere.matching.dto.request;

import com.sep.treksphere.common.constant.MessageConstant;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Request mở biểu quyết giải tán nhóm — luôn tạo với {@code voteType = GROUP_DISSOLUTION}
 * và đúng 2 option cố định theo thứ tự ("Đồng ý" = optionOrder 1, "Không đồng ý" =
 * optionOrder 2) do server tự dựng. Client chỉ nhập lý do + thời hạn, không tự đặt option,
 * vì optionOrder = 1 thắng sẽ kích hoạt side effect huỷ nhóm.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpenDissolutionVoteRequest {

    @NotBlank(message = MessageConstant.GROUP_VOTE_REASON_REQUIRED)
    private String reason;

    @NotNull(message = MessageConstant.GROUP_VOTE_CLOSES_AT_REQUIRED)
    @Future(message = MessageConstant.GROUP_VOTE_CLOSES_AT_MUST_BE_FUTURE)
    private LocalDateTime closesAt;
}
