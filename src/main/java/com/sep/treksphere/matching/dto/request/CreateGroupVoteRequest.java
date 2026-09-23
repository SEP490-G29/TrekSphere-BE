package com.sep.treksphere.matching.dto.request;

import com.sep.treksphere.common.constant.MessageConstant;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Request mở 1 bình chọn chung (poll) trong nhóm — luôn tạo với {@code voteType = OTHER}.
 * LEADER_ELECTION và GROUP_DISSOLUTION không dùng request này — server tự dựng option cố
 * định qua các method riêng (xem {@code openLeaderElectionVote}/{@code openDissolutionVote})
 * để tránh client tự ý cấu trúc option cho 2 loại vote có side effect đặc quyền.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateGroupVoteRequest {

    @NotBlank(message = MessageConstant.GROUP_VOTE_TITLE_REQUIRED)
    @Size(max = 200, message = MessageConstant.GROUP_VOTE_TITLE_TOO_LONG)
    private String title;

    private String reason;

    @NotNull(message = MessageConstant.GROUP_VOTE_CLOSES_AT_REQUIRED)
    @Future(message = MessageConstant.GROUP_VOTE_CLOSES_AT_MUST_BE_FUTURE)
    private LocalDateTime closesAt;

    @NotEmpty(message = MessageConstant.GROUP_VOTE_OPTIONS_MIN_COUNT)
    @Size(min = 2, message = MessageConstant.GROUP_VOTE_OPTIONS_MIN_COUNT)
    private List<@NotBlank @Size(max = 200) String> optionLabels;
}
