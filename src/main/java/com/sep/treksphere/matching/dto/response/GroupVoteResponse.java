package com.sep.treksphere.matching.dto.response;

import com.sep.treksphere.matching.enums.VoteStatus;
import com.sep.treksphere.matching.enums.VoteType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupVoteResponse {

    private UUID groupVoteId;
    private UUID matchingGroupId;
    private VoteType voteType;
    private String title;
    private String reason;
    private UUID createdByMemberId;
    private String createdByName;
    private VoteStatus status;
    private LocalDateTime opensAt;
    private LocalDateTime closesAt;
    private Integer eligibleVoterCount;
    private UUID winningOptionId;
    private LocalDateTime closedAt;
    private List<GroupVoteOptionResponse> options;
    /** Option mà người xem hiện tại đã bỏ phiếu, null nếu chưa vote. */
    private UUID myBallotOptionId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
