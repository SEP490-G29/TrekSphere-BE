package com.sep.treksphere.matching.dto.request;

import com.sep.treksphere.common.constant.MessageConstant;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Request mở cuộc bầu Trưởng nhóm mới — luôn tạo với {@code voteType = LEADER_ELECTION}.
 * Server tự dựng title cố định và option (mỗi candidate 1 option, theo thứ tự trong
 * {@code candidateMemberIds}) — client chỉ chọn candidate, không tự đặt option label, để
 * tránh giả mạo cấu trúc option cho loại vote có side effect đổi Leader.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpenLeaderElectionRequest {

    @NotBlank(message = MessageConstant.GROUP_VOTE_REASON_REQUIRED)
    private String reason;

    @NotNull(message = MessageConstant.GROUP_VOTE_CLOSES_AT_REQUIRED)
    @Future(message = MessageConstant.GROUP_VOTE_CLOSES_AT_MUST_BE_FUTURE)
    private LocalDateTime closesAt;

    @NotEmpty(message = MessageConstant.GROUP_VOTE_OPTIONS_MIN_COUNT)
    private List<UUID> candidateMemberIds;
}
