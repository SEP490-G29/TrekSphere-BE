package com.sep.treksphere.matching.service;

import com.sep.treksphere.matching.dto.request.CastBallotRequest;
import com.sep.treksphere.matching.dto.request.CreateGroupVoteRequest;
import com.sep.treksphere.matching.dto.request.OpenDissolutionVoteRequest;
import com.sep.treksphere.matching.dto.request.OpenLeaderElectionRequest;
import com.sep.treksphere.matching.dto.response.GroupVoteResponse;
import com.sep.treksphere.matching.entity.MatchingMember;
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

    /**
     * Mở biểu quyết giải tán nhóm (voteType = GROUP_DISSOLUTION), luôn tạo đúng 2 option cố
     * định ("Đồng ý" order 1, "Không đồng ý" order 2). Khi đóng có winner là "Đồng ý", side
     * effect atomic chuyển group sang CANCELLED và huỷ GroupTrip đang PLANNED (nếu có).
     */
    GroupVoteResponse openDissolutionVote(UUID groupId, OpenDissolutionVoteRequest request, UUID currentUserId);

    Page<GroupVoteResponse> getVotes(
            UUID groupId, VoteType voteType, VoteStatus status, Pageable pageable, UUID currentUserId);

    GroupVoteResponse getVoteDetail(UUID groupId, UUID voteId, UUID currentUserId);

    GroupVoteResponse castBallot(UUID groupId, UUID voteId, CastBallotRequest request, UUID currentUserId);

    /** Đóng khi đến hạn hoặc đã đủ phiếu; retry trên vote đã CLOSED là idempotent. */
    GroupVoteResponse closeVote(UUID groupId, UUID voteId, UUID currentUserId);

    /** Đóng sớm bởi người mở vote hoặc Leader, không tính kết quả, không side effect. */
    GroupVoteResponse cancelVote(UUID groupId, UUID voteId, UUID currentUserId);

    /**
     * P4-S5: gọi trong cùng transaction ngay sau khi {@code member} chuyển LEFT/REMOVED (từ
     * {@code leaveMatchingGroup}/{@code removeMember}). Với mọi vote đang OPEN mà member này đã
     * có ballot: xoá ballot và giảm {@code eligibleVoterCount} 1 đơn vị (member không voted thì
     * không đổi gì — vẫn tính vào mẫu số, chỉ đóng được qua deadline), sau đó re-check điều kiện
     * đóng (đến hạn hoặc đủ phiếu) để trigger close ngay nếu cần.
     */
    void handleMemberEligibilityLoss(MatchingMember member);
}
