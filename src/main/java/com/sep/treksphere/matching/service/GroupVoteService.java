package com.sep.treksphere.matching.service;

import com.sep.treksphere.matching.dto.request.CastBallotRequest;
import com.sep.treksphere.matching.dto.request.CreateGroupVoteRequest;
import com.sep.treksphere.matching.dto.request.OpenLeaderElectionRequest;
import com.sep.treksphere.matching.dto.response.GroupVoteResponse;
import com.sep.treksphere.matching.enums.VoteStatus;
import com.sep.treksphere.matching.enums.VoteType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface GroupVoteService {

    /** Mở 1 bình chọn chung (voteType = OTHER) — dùng cho poll workspace (P5-S4). */
    GroupVoteResponse createGeneralPoll(UUID groupId, CreateGroupVoteRequest request, UUID currentUserId);

    /**
     * Mở cuộc bầu Trưởng nhóm mới (voteType = LEADER_ELECTION). Khi đóng có winner, side
     * effect atomic đổi Leader cũ -> MEMBER và winner -> LEADER; không đổi owner_id.
     */
    GroupVoteResponse openLeaderElectionVote(UUID groupId, OpenLeaderElectionRequest request, UUID currentUserId);

    Page<GroupVoteResponse> getVotes(
            UUID groupId, VoteType voteType, VoteStatus status, Pageable pageable, UUID currentUserId);

    GroupVoteResponse getVoteDetail(UUID groupId, UUID voteId, UUID currentUserId);

    GroupVoteResponse castBallot(UUID groupId, UUID voteId, CastBallotRequest request, UUID currentUserId);

    /** Đóng khi đến hạn hoặc đã đủ phiếu; retry trên vote đã CLOSED là idempotent. */
    GroupVoteResponse closeVote(UUID groupId, UUID voteId, UUID currentUserId);

    /** Đóng sớm bởi người mở vote hoặc Leader, không tính kết quả, không side effect. */
    GroupVoteResponse cancelVote(UUID groupId, UUID voteId, UUID currentUserId);
}
