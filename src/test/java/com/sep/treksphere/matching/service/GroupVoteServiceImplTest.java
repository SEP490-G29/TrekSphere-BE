package com.sep.treksphere.matching.service;

import com.sep.treksphere.common.exception.AppException;
import com.sep.treksphere.common.exception.ErrorCode;
import com.sep.treksphere.matching.dto.request.CastBallotRequest;
import com.sep.treksphere.matching.dto.request.CreateGroupVoteRequest;
import com.sep.treksphere.matching.dto.request.OpenDissolutionVoteRequest;
import com.sep.treksphere.matching.dto.request.OpenLeaderElectionRequest;
import com.sep.treksphere.matching.dto.response.GroupVoteResponse;
import com.sep.treksphere.matching.entity.GroupTrip;
import com.sep.treksphere.matching.entity.GroupVote;
import com.sep.treksphere.matching.entity.GroupVoteBallot;
import com.sep.treksphere.matching.entity.GroupVoteOption;
import com.sep.treksphere.matching.entity.MatchingGroup;
import com.sep.treksphere.matching.entity.MatchingMember;
import com.sep.treksphere.matching.enums.GroupTripStatus;
import com.sep.treksphere.matching.enums.JoinStatus;
import com.sep.treksphere.matching.enums.MatchingGroupStatus;
import com.sep.treksphere.matching.enums.MatchingRole;
import com.sep.treksphere.matching.enums.VoteStatus;
import com.sep.treksphere.matching.enums.VoteType;
import com.sep.treksphere.matching.repository.GroupTripRepository;
import com.sep.treksphere.matching.repository.GroupVoteBallotRepository;
import com.sep.treksphere.matching.repository.GroupVoteOptionRepository;
import com.sep.treksphere.matching.repository.GroupVoteRepository;
import com.sep.treksphere.matching.repository.MatchingGroupRepository;
import com.sep.treksphere.matching.repository.MatchingMemberRepository;
import com.sep.treksphere.matching.service.impl.GroupVoteServiceImpl;
import com.sep.treksphere.notification.service.NotificationService;
import com.sep.treksphere.user.entity.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GroupVoteServiceImplTest {

    @Mock
    private GroupVoteRepository groupVoteRepository;

    @Mock
    private GroupVoteOptionRepository groupVoteOptionRepository;

    @Mock
    private GroupVoteBallotRepository groupVoteBallotRepository;

    @Mock
    private MatchingMemberRepository matchingMemberRepository;

    @Mock
    private MatchingGroupRepository matchingGroupRepository;

    @Mock
    private GroupTripRepository groupTripRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private GroupSettlementService groupSettlementService;

    @InjectMocks
    private GroupVoteServiceImpl groupVoteService;

    private UUID groupId;
    private MatchingGroup group;
    private User leaderUser;
    private User memberUser;
    private User otherMemberUser;
    private MatchingMember leaderMember;
    private MatchingMember memberEntity;
    private MatchingMember otherMemberEntity;

    @BeforeEach
    void setUp() {
        TransactionSynchronizationManager.initSynchronization();

        groupId = UUID.randomUUID();
        group = new MatchingGroup();
        group.setMatchingGroupId(groupId);

        leaderUser = new User();
        leaderUser.setUserId(UUID.randomUUID());
        leaderUser.setFullName("Leader Huy");

        memberUser = new User();
        memberUser.setUserId(UUID.randomUUID());
        memberUser.setFullName("Member Nam");

        otherMemberUser = new User();
        otherMemberUser.setUserId(UUID.randomUUID());
        otherMemberUser.setFullName("Member Lan");

        leaderMember = new MatchingMember();
        leaderMember.setMatchingMemberId(UUID.randomUUID());
        leaderMember.setMatchingGroup(group);
        leaderMember.setUser(leaderUser);
        leaderMember.setRole(MatchingRole.LEADER);
        leaderMember.setStatus(JoinStatus.ACCEPTED);

        memberEntity = new MatchingMember();
        memberEntity.setMatchingMemberId(UUID.randomUUID());
        memberEntity.setMatchingGroup(group);
        memberEntity.setUser(memberUser);
        memberEntity.setRole(MatchingRole.MEMBER);
        memberEntity.setStatus(JoinStatus.ACCEPTED);

        otherMemberEntity = new MatchingMember();
        otherMemberEntity.setMatchingMemberId(UUID.randomUUID());
        otherMemberEntity.setMatchingGroup(group);
        otherMemberEntity.setUser(otherMemberUser);
        otherMemberEntity.setRole(MatchingRole.MEMBER);
        otherMemberEntity.setStatus(JoinStatus.ACCEPTED);
    }

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    private CreateGroupVoteRequest sampleCreateRequest() {
        return CreateGroupVoteRequest.builder()
                .title("Chọn quán ăn tối nay")
                .reason("Cả nhóm cùng quyết định")
                .closesAt(LocalDateTime.now().plusDays(1))
                .optionLabels(List.of("Quán A", "Quán B"))
                .build();
    }

    private GroupVote newOpenVote(int eligibleVoterCount, LocalDateTime closesAt) {
        GroupVote vote = new GroupVote();
        vote.setGroupVoteId(UUID.randomUUID());
        vote.setMatchingGroup(group);
        vote.setVoteType(VoteType.OTHER);
        vote.setTitle("Chọn quán ăn tối nay");
        vote.setReason("Cả nhóm cùng quyết định");
        vote.setCreatedByMember(leaderMember);
        vote.setStatus(VoteStatus.OPEN);
        vote.setOpensAt(LocalDateTime.now());
        vote.setClosesAt(closesAt);
        vote.setEligibleVoterCount(eligibleVoterCount);
        return vote;
    }

    private GroupVoteOption newOption(GroupVote vote, int order, String label) {
        GroupVoteOption option = new GroupVoteOption();
        option.setGroupVoteOptionId(UUID.randomUUID());
        option.setGroupVote(vote);
        option.setOptionOrder(order);
        option.setOptionLabel(label);
        return option;
    }

    @Test
    @DisplayName("createGeneralPoll: happy path tạo vote OPEN với option đúng thứ tự")
    void createGeneralPoll_HappyPath_CreatesOpenVote() {
        when(matchingMemberRepository.findByGroupIdAndUserId(groupId, leaderUser.getUserId()))
                .thenReturn(Optional.of(leaderMember));
        when(groupVoteRepository.existsByMatchingGroup_MatchingGroupIdAndVoteTypeAndStatusAndIsDeletedFalse(
                groupId, VoteType.OTHER, VoteStatus.OPEN)).thenReturn(false);
        when(matchingMemberRepository.countActiveMembersByGroupIdAndStatus(groupId, JoinStatus.ACCEPTED))
                .thenReturn(3L);

        GroupVote savedVote = newOpenVote(3, LocalDateTime.now().plusDays(1));
        when(groupVoteRepository.save(any(GroupVote.class))).thenReturn(savedVote);
        when(groupVoteOptionRepository.save(any(GroupVoteOption.class)))
                .thenAnswer(inv -> {
                    GroupVoteOption option = inv.getArgument(0);
                    option.setGroupVoteOptionId(UUID.randomUUID());
                    return option;
                });
        when(matchingMemberRepository.findActiveMembers(groupId, JoinStatus.ACCEPTED))
                .thenReturn(List.of(leaderMember, memberEntity, otherMemberEntity));

        GroupVoteResponse response = groupVoteService.createGeneralPoll(groupId, sampleCreateRequest(), leaderUser.getUserId());

        assertThat(response.getStatus()).isEqualTo(VoteStatus.OPEN);
        assertThat(response.getVoteType()).isEqualTo(VoteType.OTHER);
        assertThat(response.getOptions()).hasSize(2);
        assertThat(response.getOptions().get(0).getOptionOrder()).isEqualTo(1);
        assertThat(response.getEligibleVoterCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("createGeneralPoll: caller không phải ACCEPTED member -> NOT_ACCEPTED_MATCHING_MEMBER")
    void createGeneralPoll_CallerNotActiveMember_ThrowsError() {
        when(matchingMemberRepository.findByGroupIdAndUserId(groupId, memberUser.getUserId()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> groupVoteService.createGeneralPoll(groupId, sampleCreateRequest(), memberUser.getUserId()))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_ACCEPTED_MATCHING_MEMBER);

        verify(groupVoteRepository, never()).save(any());
    }

    @Test
    @DisplayName("createGeneralPoll: đã có vote OTHER đang OPEN -> GROUP_VOTE_DUPLICATE_OPEN_TYPE")
    void createGeneralPoll_DuplicateOpenType_ThrowsError() {
        when(matchingMemberRepository.findByGroupIdAndUserId(groupId, leaderUser.getUserId()))
                .thenReturn(Optional.of(leaderMember));
        when(groupVoteRepository.existsByMatchingGroup_MatchingGroupIdAndVoteTypeAndStatusAndIsDeletedFalse(
                groupId, VoteType.OTHER, VoteStatus.OPEN)).thenReturn(true);

        assertThatThrownBy(() -> groupVoteService.createGeneralPoll(groupId, sampleCreateRequest(), leaderUser.getUserId()))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.GROUP_VOTE_DUPLICATE_OPEN_TYPE);

        verify(groupVoteRepository, never()).save(any());
    }

    @Test
    @DisplayName("castBallot: option không thuộc vote -> GROUP_VOTE_INVALID_OPTION")
    void castBallot_OptionNotInVote_ThrowsError() {
        GroupVote vote = newOpenVote(3, LocalDateTime.now().plusDays(1));
        UUID foreignOptionId = UUID.randomUUID();

        when(matchingMemberRepository.findByGroupIdAndUserId(groupId, memberUser.getUserId()))
                .thenReturn(Optional.of(memberEntity));
        when(groupVoteRepository.findByIdForUpdate(vote.getGroupVoteId())).thenReturn(Optional.of(vote));
        when(groupVoteOptionRepository.findByGroupVoteOptionIdAndGroupVote_GroupVoteIdAndIsDeletedFalse(
                foreignOptionId, vote.getGroupVoteId())).thenReturn(Optional.empty());

        CastBallotRequest request = CastBallotRequest.builder().optionId(foreignOptionId).build();

        assertThatThrownBy(() -> groupVoteService.castBallot(groupId, vote.getGroupVoteId(), request, memberUser.getUserId()))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.GROUP_VOTE_INVALID_OPTION);

        verify(groupVoteBallotRepository, never()).save(any());
    }

    @Test
    @DisplayName("castBallot: đã bỏ phiếu rồi, chọn option KHÁC -> cập nhật ballot cũ sang option mới (đổi phiếu)")
    void castBallot_AlreadyVoted_DifferentOption_UpdatesExistingBallot() {
        GroupVote vote = newOpenVote(3, LocalDateTime.now().plusDays(1));
        GroupVoteOption optionA = newOption(vote, 1, "Quán A");
        GroupVoteOption optionB = newOption(vote, 2, "Quán B");

        GroupVoteBallot existingBallot = new GroupVoteBallot();
        existingBallot.setGroupVote(vote);
        existingBallot.setGroupVoteOption(optionA);
        existingBallot.setVoterMatchingMember(memberEntity);

        when(matchingMemberRepository.findByGroupIdAndUserId(groupId, memberUser.getUserId()))
                .thenReturn(Optional.of(memberEntity));
        when(groupVoteRepository.findByIdForUpdate(vote.getGroupVoteId())).thenReturn(Optional.of(vote));
        when(groupVoteOptionRepository.findByGroupVoteOptionIdAndGroupVote_GroupVoteIdAndIsDeletedFalse(
                optionB.getGroupVoteOptionId(), vote.getGroupVoteId())).thenReturn(Optional.of(optionB));
        when(groupVoteBallotRepository.findByGroupVoteAndVoterMatchingMember(vote, memberEntity))
                .thenReturn(Optional.of(existingBallot));
        when(groupVoteBallotRepository.countByGroupVote(vote)).thenReturn(1L);

        CastBallotRequest request = CastBallotRequest.builder().optionId(optionB.getGroupVoteOptionId()).build();
        groupVoteService.castBallot(groupId, vote.getGroupVoteId(), request, memberUser.getUserId());

        assertThat(existingBallot.getGroupVoteOption()).isEqualTo(optionB);
        verify(groupVoteBallotRepository).save(existingBallot);
    }

    @Test
    @DisplayName("castBallot: đã bỏ phiếu rồi, chọn LẠI đúng option cũ -> no-op, không save lại")
    void castBallot_AlreadyVoted_SameOption_NoOp() {
        GroupVote vote = newOpenVote(3, LocalDateTime.now().plusDays(1));
        GroupVoteOption optionA = newOption(vote, 1, "Quán A");

        GroupVoteBallot existingBallot = new GroupVoteBallot();
        existingBallot.setGroupVote(vote);
        existingBallot.setGroupVoteOption(optionA);
        existingBallot.setVoterMatchingMember(memberEntity);

        when(matchingMemberRepository.findByGroupIdAndUserId(groupId, memberUser.getUserId()))
                .thenReturn(Optional.of(memberEntity));
        when(groupVoteRepository.findByIdForUpdate(vote.getGroupVoteId())).thenReturn(Optional.of(vote));
        when(groupVoteOptionRepository.findByGroupVoteOptionIdAndGroupVote_GroupVoteIdAndIsDeletedFalse(
                optionA.getGroupVoteOptionId(), vote.getGroupVoteId())).thenReturn(Optional.of(optionA));
        when(groupVoteBallotRepository.findByGroupVoteAndVoterMatchingMember(vote, memberEntity))
                .thenReturn(Optional.of(existingBallot));
        when(groupVoteBallotRepository.countByGroupVote(vote)).thenReturn(1L);

        CastBallotRequest request = CastBallotRequest.builder().optionId(optionA.getGroupVoteOptionId()).build();
        groupVoteService.castBallot(groupId, vote.getGroupVoteId(), request, memberUser.getUserId());

        verify(groupVoteBallotRepository, never()).save(any());
    }

    @Test
    @DisplayName("castBallot: vote đã CLOSED -> GROUP_VOTE_CLOSED")
    void castBallot_VoteClosed_ThrowsError() {
        GroupVote vote = newOpenVote(3, LocalDateTime.now().plusDays(1));
        vote.setStatus(VoteStatus.CLOSED);

        when(matchingMemberRepository.findByGroupIdAndUserId(groupId, memberUser.getUserId()))
                .thenReturn(Optional.of(memberEntity));
        when(groupVoteRepository.findByIdForUpdate(vote.getGroupVoteId())).thenReturn(Optional.of(vote));

        CastBallotRequest request = CastBallotRequest.builder().optionId(UUID.randomUUID()).build();

        assertThatThrownBy(() -> groupVoteService.castBallot(groupId, vote.getGroupVoteId(), request, memberUser.getUserId()))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.GROUP_VOTE_CLOSED);
    }

    @Test
    @DisplayName("castBallot: đã qua deadline -> tự đóng vote (tie, không có winner) thay vì nhận ballot")
    void castBallot_DeadlinePassed_AutoClosesInstead() {
        GroupVote vote = newOpenVote(3, LocalDateTime.now().minusMinutes(1));
        GroupVoteOption optionA = newOption(vote, 1, "Quán A");
        GroupVoteOption optionB = newOption(vote, 2, "Quán B");

        when(matchingMemberRepository.findByGroupIdAndUserId(groupId, memberUser.getUserId()))
                .thenReturn(Optional.of(memberEntity));
        when(groupVoteRepository.findByIdForUpdate(vote.getGroupVoteId())).thenReturn(Optional.of(vote));
        when(groupVoteOptionRepository.findOptionsWithCandidateByVoteId(vote.getGroupVoteId()))
                .thenReturn(List.of(optionA, optionB));
        when(groupVoteBallotRepository.countByGroupVoteAndGroupVoteOption(vote, optionA)).thenReturn(0L);
        when(groupVoteBallotRepository.countByGroupVoteAndGroupVoteOption(vote, optionB)).thenReturn(0L);
        when(groupVoteRepository.save(vote)).thenReturn(vote);
        when(matchingMemberRepository.findActiveMembers(groupId, JoinStatus.ACCEPTED))
                .thenReturn(List.of(leaderMember, memberEntity, otherMemberEntity));

        CastBallotRequest request = CastBallotRequest.builder().optionId(optionA.getGroupVoteOptionId()).build();
        GroupVoteResponse response = groupVoteService.castBallot(groupId, vote.getGroupVoteId(), request, memberUser.getUserId());

        assertThat(response.getStatus()).isEqualTo(VoteStatus.CLOSED);
        assertThat(response.getWinningOptionId()).isNull();
        verify(groupVoteBallotRepository, never()).save(any());
    }

    @Test
    @DisplayName("castBallot: đủ eligibleVoterCount -> tự đóng, xác định đúng winner theo plurality")
    void castBallot_AllVoted_AutoClosesWithWinner() {
        GroupVote vote = newOpenVote(3, LocalDateTime.now().plusDays(1));
        GroupVoteOption optionA = newOption(vote, 1, "Quán A");
        GroupVoteOption optionB = newOption(vote, 2, "Quán B");

        when(matchingMemberRepository.findByGroupIdAndUserId(groupId, otherMemberUser.getUserId()))
                .thenReturn(Optional.of(otherMemberEntity));
        when(groupVoteRepository.findByIdForUpdate(vote.getGroupVoteId())).thenReturn(Optional.of(vote));
        when(groupVoteOptionRepository.findByGroupVoteOptionIdAndGroupVote_GroupVoteIdAndIsDeletedFalse(
                optionA.getGroupVoteOptionId(), vote.getGroupVoteId())).thenReturn(Optional.of(optionA));
        // Trước ballot cuối: đã có 2/3 phiếu (A=2). Sau khi lưu ballot thứ 3 (A) -> count = 3 = eligible -> auto-close.
        when(groupVoteBallotRepository.countByGroupVote(vote)).thenReturn(3L);
        when(groupVoteOptionRepository.findOptionsWithCandidateByVoteId(vote.getGroupVoteId()))
                .thenReturn(List.of(optionA, optionB));
        when(groupVoteBallotRepository.countByGroupVoteAndGroupVoteOption(vote, optionA)).thenReturn(3L);
        when(groupVoteBallotRepository.countByGroupVoteAndGroupVoteOption(vote, optionB)).thenReturn(0L);
        when(groupVoteRepository.save(vote)).thenReturn(vote);
        when(matchingMemberRepository.findActiveMembers(groupId, JoinStatus.ACCEPTED))
                .thenReturn(List.of(leaderMember, memberEntity, otherMemberEntity));

        CastBallotRequest request = CastBallotRequest.builder().optionId(optionA.getGroupVoteOptionId()).build();
        GroupVoteResponse response = groupVoteService.castBallot(groupId, vote.getGroupVoteId(), request, otherMemberUser.getUserId());

        assertThat(response.getStatus()).isEqualTo(VoteStatus.CLOSED);
        assertThat(response.getWinningOptionId()).isEqualTo(optionA.getGroupVoteOptionId());
        verify(groupVoteBallotRepository).save(any(GroupVoteBallot.class));
    }

    @Test
    @DisplayName("closeVote: chưa tới hạn và chưa đủ phiếu -> GROUP_VOTE_NOT_READY_TO_CLOSE")
    void closeVote_NotReady_ThrowsError() {
        GroupVote vote = newOpenVote(3, LocalDateTime.now().plusDays(1));

        when(matchingMemberRepository.findByGroupIdAndUserId(groupId, leaderUser.getUserId()))
                .thenReturn(Optional.of(leaderMember));
        when(groupVoteRepository.findByIdForUpdate(vote.getGroupVoteId())).thenReturn(Optional.of(vote));
        when(groupVoteBallotRepository.countByGroupVote(vote)).thenReturn(1L);

        assertThatThrownBy(() -> groupVoteService.closeVote(groupId, vote.getGroupVoteId(), leaderUser.getUserId()))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.GROUP_VOTE_NOT_READY_TO_CLOSE);
    }

    @Test
    @DisplayName("closeVote: retry trên vote đã CLOSED -> idempotent, trả lại kết quả hiện tại, không lỗi")
    void closeVote_AlreadyClosed_IsIdempotent() {
        GroupVote vote = newOpenVote(3, LocalDateTime.now().plusDays(1));
        vote.setStatus(VoteStatus.CLOSED);
        vote.setClosedAt(LocalDateTime.now());

        when(matchingMemberRepository.findByGroupIdAndUserId(groupId, leaderUser.getUserId()))
                .thenReturn(Optional.of(leaderMember));
        when(groupVoteRepository.findByIdForUpdate(vote.getGroupVoteId())).thenReturn(Optional.of(vote));
        when(groupVoteOptionRepository.findOptionsWithCandidateByVoteId(vote.getGroupVoteId()))
                .thenReturn(List.of());

        GroupVoteResponse response = groupVoteService.closeVote(groupId, vote.getGroupVoteId(), leaderUser.getUserId());

        assertThat(response.getStatus()).isEqualTo(VoteStatus.CLOSED);
        verify(groupVoteRepository, never()).save(any());
    }

    @Test
    @DisplayName("closeVote: đồng hạng cao nhất -> đóng với winningOptionId = null, không side effect")
    void closeVote_Tie_WinnerIsNull() {
        GroupVote vote = newOpenVote(2, LocalDateTime.now().minusMinutes(1));
        GroupVoteOption optionA = newOption(vote, 1, "Quán A");
        GroupVoteOption optionB = newOption(vote, 2, "Quán B");

        when(matchingMemberRepository.findByGroupIdAndUserId(groupId, leaderUser.getUserId()))
                .thenReturn(Optional.of(leaderMember));
        when(groupVoteRepository.findByIdForUpdate(vote.getGroupVoteId())).thenReturn(Optional.of(vote));
        when(groupVoteBallotRepository.countByGroupVote(vote)).thenReturn(2L);
        when(groupVoteOptionRepository.findOptionsWithCandidateByVoteId(vote.getGroupVoteId()))
                .thenReturn(List.of(optionA, optionB));
        when(groupVoteBallotRepository.countByGroupVoteAndGroupVoteOption(vote, optionA)).thenReturn(1L);
        when(groupVoteBallotRepository.countByGroupVoteAndGroupVoteOption(vote, optionB)).thenReturn(1L);
        when(groupVoteRepository.save(vote)).thenReturn(vote);
        when(matchingMemberRepository.findActiveMembers(groupId, JoinStatus.ACCEPTED))
                .thenReturn(List.of(leaderMember, memberEntity));

        GroupVoteResponse response = groupVoteService.closeVote(groupId, vote.getGroupVoteId(), leaderUser.getUserId());

        assertThat(response.getStatus()).isEqualTo(VoteStatus.CLOSED);
        assertThat(response.getWinningOptionId()).isNull();
    }

    @Test
    @DisplayName("cancelVote: không phải người mở vote, không phải Leader -> MATCHING_GROUP_UNAUTHORIZED_MANAGE")
    void cancelVote_ByUnrelatedMember_ThrowsError() {
        GroupVote vote = newOpenVote(3, LocalDateTime.now().plusDays(1)); // createdByMember = leaderMember

        when(matchingMemberRepository.findByGroupIdAndUserId(groupId, otherMemberUser.getUserId()))
                .thenReturn(Optional.of(otherMemberEntity));
        when(groupVoteRepository.findByIdForUpdate(vote.getGroupVoteId())).thenReturn(Optional.of(vote));

        assertThatThrownBy(() -> groupVoteService.cancelVote(groupId, vote.getGroupVoteId(), otherMemberUser.getUserId()))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MATCHING_GROUP_UNAUTHORIZED_MANAGE);

        verify(groupVoteRepository, never()).save(any());
        assertThat(vote.getStatus()).isEqualTo(VoteStatus.OPEN);
    }

    @Test
    @DisplayName("cancelVote: người mở vote tự huỷ sớm -> CLOSED, winningOptionId = null, không side effect")
    void cancelVote_ByOpener_Succeeds() {
        GroupVote vote = newOpenVote(3, LocalDateTime.now().plusDays(1));

        when(matchingMemberRepository.findByGroupIdAndUserId(groupId, leaderUser.getUserId()))
                .thenReturn(Optional.of(leaderMember));
        when(groupVoteRepository.findByIdForUpdate(vote.getGroupVoteId())).thenReturn(Optional.of(vote));
        when(groupVoteRepository.save(vote)).thenReturn(vote);
        when(groupVoteOptionRepository.findOptionsWithCandidateByVoteId(vote.getGroupVoteId()))
                .thenReturn(List.of());
        when(matchingMemberRepository.findActiveMembers(groupId, JoinStatus.ACCEPTED))
                .thenReturn(List.of(leaderMember, memberEntity));

        GroupVoteResponse response = groupVoteService.cancelVote(groupId, vote.getGroupVoteId(), leaderUser.getUserId());

        assertThat(response.getStatus()).isEqualTo(VoteStatus.CLOSED);
        assertThat(response.getWinningOptionId()).isNull();
    }

    private OpenLeaderElectionRequest sampleElectionRequest(UUID... candidateIds) {
        return OpenLeaderElectionRequest.builder()
                .reason("Trưởng nhóm cũ bận, cần người thay thế")
                .closesAt(LocalDateTime.now().plusDays(1))
                .candidateMemberIds(List.of(candidateIds))
                .build();
    }

    @Test
    @DisplayName("openLeaderElectionVote: happy path tạo vote LEADER_ELECTION với option gắn candidate")
    void openLeaderElectionVote_HappyPath_CreatesOptionsWithCandidates() {
        when(matchingMemberRepository.findByGroupIdAndUserId(groupId, memberUser.getUserId()))
                .thenReturn(Optional.of(memberEntity));
        when(groupVoteRepository.existsByMatchingGroup_MatchingGroupIdAndVoteTypeAndStatusAndIsDeletedFalse(
                groupId, VoteType.LEADER_ELECTION, VoteStatus.OPEN)).thenReturn(false);
        when(matchingMemberRepository.findMemberByIdAndGroupId(memberEntity.getMatchingMemberId(), groupId))
                .thenReturn(Optional.of(memberEntity));
        when(matchingMemberRepository.findMemberByIdAndGroupId(otherMemberEntity.getMatchingMemberId(), groupId))
                .thenReturn(Optional.of(otherMemberEntity));
        when(matchingMemberRepository.countActiveMembersByGroupIdAndStatus(groupId, JoinStatus.ACCEPTED))
                .thenReturn(3L);

        GroupVote savedVote = newOpenVote(3, LocalDateTime.now().plusDays(1));
        savedVote.setVoteType(VoteType.LEADER_ELECTION);
        when(groupVoteRepository.save(any(GroupVote.class))).thenReturn(savedVote);
        when(groupVoteOptionRepository.save(any(GroupVoteOption.class)))
                .thenAnswer(inv -> {
                    GroupVoteOption option = inv.getArgument(0);
                    option.setGroupVoteOptionId(UUID.randomUUID());
                    return option;
                });
        when(matchingMemberRepository.findActiveMembers(groupId, JoinStatus.ACCEPTED))
                .thenReturn(List.of(leaderMember, memberEntity, otherMemberEntity));

        OpenLeaderElectionRequest request = sampleElectionRequest(
                memberEntity.getMatchingMemberId(), otherMemberEntity.getMatchingMemberId());
        GroupVoteResponse response = groupVoteService.openLeaderElectionVote(groupId, request, memberUser.getUserId());

        assertThat(response.getVoteType()).isEqualTo(VoteType.LEADER_ELECTION);
        assertThat(response.getOptions()).hasSize(2);
        assertThat(response.getOptions().get(0).getCandidateMemberId()).isEqualTo(memberEntity.getMatchingMemberId());
        assertThat(response.getOptions().get(1).getCandidateMemberId()).isEqualTo(otherMemberEntity.getMatchingMemberId());
    }

    @Test
    @DisplayName("openLeaderElectionVote: chỉ 1 candidate -> GROUP_VOTE_INVALID_OPTION_COUNT")
    void openLeaderElectionVote_LessThanTwoCandidates_ThrowsError() {
        when(matchingMemberRepository.findByGroupIdAndUserId(groupId, memberUser.getUserId()))
                .thenReturn(Optional.of(memberEntity));
        when(groupVoteRepository.existsByMatchingGroup_MatchingGroupIdAndVoteTypeAndStatusAndIsDeletedFalse(
                groupId, VoteType.LEADER_ELECTION, VoteStatus.OPEN)).thenReturn(false);

        OpenLeaderElectionRequest request = sampleElectionRequest(memberEntity.getMatchingMemberId());

        assertThatThrownBy(() -> groupVoteService.openLeaderElectionVote(groupId, request, memberUser.getUserId()))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.GROUP_VOTE_INVALID_OPTION_COUNT);

        verify(groupVoteRepository, never()).save(any());
    }

    @Test
    @DisplayName("openLeaderElectionVote: candidate đang là Leader -> GROUP_VOTE_INVALID_CANDIDATE")
    void openLeaderElectionVote_CandidateAlreadyLeader_ThrowsError() {
        when(matchingMemberRepository.findByGroupIdAndUserId(groupId, memberUser.getUserId()))
                .thenReturn(Optional.of(memberEntity));
        when(groupVoteRepository.existsByMatchingGroup_MatchingGroupIdAndVoteTypeAndStatusAndIsDeletedFalse(
                groupId, VoteType.LEADER_ELECTION, VoteStatus.OPEN)).thenReturn(false);
        when(matchingMemberRepository.findMemberByIdAndGroupId(memberEntity.getMatchingMemberId(), groupId))
                .thenReturn(Optional.of(memberEntity));
        when(matchingMemberRepository.findMemberByIdAndGroupId(leaderMember.getMatchingMemberId(), groupId))
                .thenReturn(Optional.of(leaderMember));

        OpenLeaderElectionRequest request = sampleElectionRequest(
                memberEntity.getMatchingMemberId(), leaderMember.getMatchingMemberId());

        assertThatThrownBy(() -> groupVoteService.openLeaderElectionVote(groupId, request, memberUser.getUserId()))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.GROUP_VOTE_INVALID_CANDIDATE);

        verify(groupVoteRepository, never()).save(any());
    }

    @Test
    @DisplayName("openLeaderElectionVote: candidate không thuộc group -> GROUP_VOTE_INVALID_CANDIDATE")
    void openLeaderElectionVote_CandidateNotInGroup_ThrowsError() {
        UUID foreignCandidateId = UUID.randomUUID();

        when(matchingMemberRepository.findByGroupIdAndUserId(groupId, memberUser.getUserId()))
                .thenReturn(Optional.of(memberEntity));
        when(groupVoteRepository.existsByMatchingGroup_MatchingGroupIdAndVoteTypeAndStatusAndIsDeletedFalse(
                groupId, VoteType.LEADER_ELECTION, VoteStatus.OPEN)).thenReturn(false);
        when(matchingMemberRepository.findMemberByIdAndGroupId(memberEntity.getMatchingMemberId(), groupId))
                .thenReturn(Optional.of(memberEntity));
        when(matchingMemberRepository.findMemberByIdAndGroupId(foreignCandidateId, groupId))
                .thenReturn(Optional.empty());

        OpenLeaderElectionRequest request = sampleElectionRequest(
                memberEntity.getMatchingMemberId(), foreignCandidateId);

        assertThatThrownBy(() -> groupVoteService.openLeaderElectionVote(groupId, request, memberUser.getUserId()))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.GROUP_VOTE_INVALID_CANDIDATE);
    }

    @Test
    @DisplayName("closeVote trên LEADER_ELECTION có winner: đổi Leader cũ -> MEMBER, winner -> LEADER atomically")
    void closeVote_ElectionWithWinner_SwapsLeaderAtomically() {
        GroupVote vote = newOpenVote(3, LocalDateTime.now().minusMinutes(1));
        vote.setVoteType(VoteType.LEADER_ELECTION);
        GroupVoteOption winnerOption = newOption(vote, 1, memberEntity.getUser().getFullName());
        winnerOption.setCandidateMatchingMember(memberEntity);
        GroupVoteOption loserOption = newOption(vote, 2, otherMemberEntity.getUser().getFullName());
        loserOption.setCandidateMatchingMember(otherMemberEntity);

        when(matchingMemberRepository.findByGroupIdAndUserId(groupId, leaderUser.getUserId()))
                .thenReturn(Optional.of(leaderMember));
        when(groupVoteRepository.findByIdForUpdate(vote.getGroupVoteId())).thenReturn(Optional.of(vote));
        when(groupVoteBallotRepository.countByGroupVote(vote)).thenReturn(2L);
        when(groupVoteOptionRepository.findOptionsWithCandidateByVoteId(vote.getGroupVoteId()))
                .thenReturn(List.of(winnerOption, loserOption));
        when(groupVoteBallotRepository.countByGroupVoteAndGroupVoteOption(vote, winnerOption)).thenReturn(2L);
        when(groupVoteBallotRepository.countByGroupVoteAndGroupVoteOption(vote, loserOption)).thenReturn(0L);
        when(groupVoteRepository.save(vote)).thenReturn(vote);
        when(matchingMemberRepository.findActiveMembers(groupId, JoinStatus.ACCEPTED))
                .thenReturn(List.of(leaderMember, memberEntity, otherMemberEntity));

        GroupVoteResponse response = groupVoteService.closeVote(groupId, vote.getGroupVoteId(), leaderUser.getUserId());

        assertThat(response.getWinningOptionId()).isEqualTo(winnerOption.getGroupVoteOptionId());
        assertThat(memberEntity.getRole()).isEqualTo(MatchingRole.LEADER);
        assertThat(leaderMember.getRole()).isEqualTo(MatchingRole.MEMBER);
        verify(matchingMemberRepository).saveAndFlush(leaderMember);
        verify(matchingMemberRepository).saveAndFlush(memberEntity);
    }

    @Test
    @DisplayName("closeVote trên LEADER_ELECTION đồng hạng: không đổi Leader")
    void closeVote_ElectionTie_NoLeaderChange() {
        GroupVote vote = newOpenVote(2, LocalDateTime.now().minusMinutes(1));
        vote.setVoteType(VoteType.LEADER_ELECTION);
        GroupVoteOption optionA = newOption(vote, 1, memberEntity.getUser().getFullName());
        optionA.setCandidateMatchingMember(memberEntity);
        GroupVoteOption optionB = newOption(vote, 2, otherMemberEntity.getUser().getFullName());
        optionB.setCandidateMatchingMember(otherMemberEntity);

        when(matchingMemberRepository.findByGroupIdAndUserId(groupId, leaderUser.getUserId()))
                .thenReturn(Optional.of(leaderMember));
        when(groupVoteRepository.findByIdForUpdate(vote.getGroupVoteId())).thenReturn(Optional.of(vote));
        when(groupVoteBallotRepository.countByGroupVote(vote)).thenReturn(2L);
        when(groupVoteOptionRepository.findOptionsWithCandidateByVoteId(vote.getGroupVoteId()))
                .thenReturn(List.of(optionA, optionB));
        when(groupVoteBallotRepository.countByGroupVoteAndGroupVoteOption(vote, optionA)).thenReturn(1L);
        when(groupVoteBallotRepository.countByGroupVoteAndGroupVoteOption(vote, optionB)).thenReturn(1L);
        when(groupVoteRepository.save(vote)).thenReturn(vote);
        when(matchingMemberRepository.findActiveMembers(groupId, JoinStatus.ACCEPTED))
                .thenReturn(List.of(leaderMember, memberEntity, otherMemberEntity));

        GroupVoteResponse response = groupVoteService.closeVote(groupId, vote.getGroupVoteId(), leaderUser.getUserId());

        assertThat(response.getWinningOptionId()).isNull();
        assertThat(leaderMember.getRole()).isEqualTo(MatchingRole.LEADER);
        assertThat(memberEntity.getRole()).isEqualTo(MatchingRole.MEMBER);
        verify(matchingMemberRepository, never()).save(memberEntity);
    }

    private OpenDissolutionVoteRequest sampleDissolutionRequest() {
        return OpenDissolutionVoteRequest.builder()
                .reason("Nhóm không còn đủ người để tiếp tục chuyến đi")
                .closesAt(LocalDateTime.now().plusDays(1))
                .build();
    }

    @Test
    @DisplayName("openDissolutionVote: happy path tạo vote GROUP_DISSOLUTION với đúng 2 option cố định thứ tự")
    void openDissolutionVote_HappyPath_CreatesFixedTwoOptions() {
        when(matchingMemberRepository.findByGroupIdAndUserId(groupId, leaderUser.getUserId()))
                .thenReturn(Optional.of(leaderMember));
        when(groupVoteRepository.existsByMatchingGroup_MatchingGroupIdAndVoteTypeAndStatusAndIsDeletedFalse(
                groupId, VoteType.GROUP_DISSOLUTION, VoteStatus.OPEN)).thenReturn(false);
        when(matchingMemberRepository.countActiveMembersByGroupIdAndStatus(groupId, JoinStatus.ACCEPTED))
                .thenReturn(3L);

        GroupVote savedVote = newOpenVote(3, LocalDateTime.now().plusDays(1));
        savedVote.setVoteType(VoteType.GROUP_DISSOLUTION);
        when(groupVoteRepository.save(any(GroupVote.class))).thenReturn(savedVote);
        when(groupVoteOptionRepository.save(any(GroupVoteOption.class)))
                .thenAnswer(inv -> {
                    GroupVoteOption option = inv.getArgument(0);
                    option.setGroupVoteOptionId(UUID.randomUUID());
                    return option;
                });
        when(matchingMemberRepository.findActiveMembers(groupId, JoinStatus.ACCEPTED))
                .thenReturn(List.of(leaderMember, memberEntity, otherMemberEntity));

        GroupVoteResponse response =
                groupVoteService.openDissolutionVote(groupId, sampleDissolutionRequest(), leaderUser.getUserId());

        assertThat(response.getVoteType()).isEqualTo(VoteType.GROUP_DISSOLUTION);
        assertThat(response.getOptions()).hasSize(2);
        assertThat(response.getOptions().get(0).getOptionOrder()).isEqualTo(1);
        assertThat(response.getOptions().get(0).getOptionLabel()).isEqualTo("Đồng ý");
        assertThat(response.getOptions().get(1).getOptionOrder()).isEqualTo(2);
        assertThat(response.getOptions().get(1).getOptionLabel()).isEqualTo("Không đồng ý");
    }

    @Test
    @DisplayName("openDissolutionVote: đã có vote GROUP_DISSOLUTION đang OPEN -> GROUP_VOTE_DUPLICATE_OPEN_TYPE")
    void openDissolutionVote_DuplicateOpenType_ThrowsError() {
        when(matchingMemberRepository.findByGroupIdAndUserId(groupId, leaderUser.getUserId()))
                .thenReturn(Optional.of(leaderMember));
        when(groupVoteRepository.existsByMatchingGroup_MatchingGroupIdAndVoteTypeAndStatusAndIsDeletedFalse(
                groupId, VoteType.GROUP_DISSOLUTION, VoteStatus.OPEN)).thenReturn(true);

        assertThatThrownBy(() -> groupVoteService.openDissolutionVote(groupId, sampleDissolutionRequest(), leaderUser.getUserId()))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.GROUP_VOTE_DUPLICATE_OPEN_TYPE);

        verify(groupVoteRepository, never()).save(any());
    }

    @Test
    @DisplayName("openDissolutionVote: trip đã IN_PROGRESS -> GROUP_DISSOLUTION_TRIP_ALREADY_STARTED")
    void openDissolutionVote_TripAlreadyInProgress_ThrowsError() {
        GroupTrip trip = new GroupTrip();
        trip.setStatus(GroupTripStatus.IN_PROGRESS);

        when(matchingMemberRepository.findByGroupIdAndUserId(groupId, leaderUser.getUserId()))
                .thenReturn(Optional.of(leaderMember));
        when(groupVoteRepository.existsByMatchingGroup_MatchingGroupIdAndVoteTypeAndStatusAndIsDeletedFalse(
                groupId, VoteType.GROUP_DISSOLUTION, VoteStatus.OPEN)).thenReturn(false);
        when(groupTripRepository.findByMatchingGroup_MatchingGroupId(groupId)).thenReturn(Optional.of(trip));

        assertThatThrownBy(() -> groupVoteService.openDissolutionVote(groupId, sampleDissolutionRequest(), leaderUser.getUserId()))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.GROUP_DISSOLUTION_TRIP_ALREADY_STARTED);

        verify(groupVoteRepository, never()).save(any());
    }

    @Test
    @DisplayName("openDissolutionVote: trip còn PLANNED -> mở được bình thường")
    void openDissolutionVote_TripStillPlanned_Succeeds() {
        GroupTrip trip = new GroupTrip();
        trip.setStatus(GroupTripStatus.PLANNED);

        when(matchingMemberRepository.findByGroupIdAndUserId(groupId, leaderUser.getUserId()))
                .thenReturn(Optional.of(leaderMember));
        when(groupVoteRepository.existsByMatchingGroup_MatchingGroupIdAndVoteTypeAndStatusAndIsDeletedFalse(
                groupId, VoteType.GROUP_DISSOLUTION, VoteStatus.OPEN)).thenReturn(false);
        when(groupTripRepository.findByMatchingGroup_MatchingGroupId(groupId)).thenReturn(Optional.of(trip));
        when(matchingMemberRepository.countActiveMembersByGroupIdAndStatus(groupId, JoinStatus.ACCEPTED))
                .thenReturn(3L);

        GroupVote savedVote = newOpenVote(3, LocalDateTime.now().plusDays(1));
        savedVote.setVoteType(VoteType.GROUP_DISSOLUTION);
        when(groupVoteRepository.save(any(GroupVote.class))).thenReturn(savedVote);
        when(groupVoteOptionRepository.save(any(GroupVoteOption.class)))
                .thenAnswer(inv -> {
                    GroupVoteOption option = inv.getArgument(0);
                    option.setGroupVoteOptionId(UUID.randomUUID());
                    return option;
                });
        when(matchingMemberRepository.findActiveMembers(groupId, JoinStatus.ACCEPTED))
                .thenReturn(List.of(leaderMember, memberEntity, otherMemberEntity));

        GroupVoteResponse response =
                groupVoteService.openDissolutionVote(groupId, sampleDissolutionRequest(), leaderUser.getUserId());

        assertThat(response.getVoteType()).isEqualTo(VoteType.GROUP_DISSOLUTION);
    }

    @Test
    @DisplayName("closeVote trên GROUP_DISSOLUTION: \"Đồng ý\" thắng -> group CANCELLED, trip PLANNED bị huỷ")
    void closeVote_DissolutionAgreeWins_CancelsGroupAndPlannedTrip() {
        GroupVote vote = newOpenVote(2, LocalDateTime.now().minusMinutes(1));
        vote.setVoteType(VoteType.GROUP_DISSOLUTION);
        GroupVoteOption agreeOption = newOption(vote, 1, "Đồng ý");
        GroupVoteOption disagreeOption = newOption(vote, 2, "Không đồng ý");

        GroupTrip trip = new GroupTrip();
        trip.setStatus(GroupTripStatus.PLANNED);

        when(matchingMemberRepository.findByGroupIdAndUserId(groupId, leaderUser.getUserId()))
                .thenReturn(Optional.of(leaderMember));
        when(groupVoteRepository.findByIdForUpdate(vote.getGroupVoteId())).thenReturn(Optional.of(vote));
        when(groupVoteBallotRepository.countByGroupVote(vote)).thenReturn(2L);
        when(groupVoteOptionRepository.findOptionsWithCandidateByVoteId(vote.getGroupVoteId()))
                .thenReturn(List.of(agreeOption, disagreeOption));
        when(groupVoteBallotRepository.countByGroupVoteAndGroupVoteOption(vote, agreeOption)).thenReturn(2L);
        when(groupVoteBallotRepository.countByGroupVoteAndGroupVoteOption(vote, disagreeOption)).thenReturn(0L);
        when(groupVoteRepository.save(vote)).thenReturn(vote);
        when(matchingGroupRepository.save(group)).thenReturn(group);
        when(groupTripRepository.findByMatchingGroup_MatchingGroupId(groupId)).thenReturn(Optional.of(trip));
        when(groupTripRepository.save(trip)).thenReturn(trip);
        when(matchingMemberRepository.findActiveMembers(groupId, JoinStatus.ACCEPTED))
                .thenReturn(List.of(leaderMember, memberEntity));

        GroupVoteResponse response = groupVoteService.closeVote(groupId, vote.getGroupVoteId(), leaderUser.getUserId());

        assertThat(response.getWinningOptionId()).isEqualTo(agreeOption.getGroupVoteOptionId());
        assertThat(group.getStatus()).isEqualTo(MatchingGroupStatus.CANCELLED);
        assertThat(trip.getStatus()).isEqualTo(GroupTripStatus.CANCELLED);
        verify(matchingGroupRepository).save(group);
        verify(groupTripRepository).save(trip);
        verify(groupSettlementService).autoGenerateSettlementsOnDissolution(groupId);
    }

    @Test
    @DisplayName("closeVote trên GROUP_DISSOLUTION: \"Không đồng ý\" thắng -> group không đổi, không side effect")
    void closeVote_DissolutionDisagreeWins_NoSideEffect() {
        GroupVote vote = newOpenVote(2, LocalDateTime.now().minusMinutes(1));
        vote.setVoteType(VoteType.GROUP_DISSOLUTION);
        group.setStatus(MatchingGroupStatus.OPEN);
        GroupVoteOption agreeOption = newOption(vote, 1, "Đồng ý");
        GroupVoteOption disagreeOption = newOption(vote, 2, "Không đồng ý");

        when(matchingMemberRepository.findByGroupIdAndUserId(groupId, leaderUser.getUserId()))
                .thenReturn(Optional.of(leaderMember));
        when(groupVoteRepository.findByIdForUpdate(vote.getGroupVoteId())).thenReturn(Optional.of(vote));
        when(groupVoteBallotRepository.countByGroupVote(vote)).thenReturn(2L);
        when(groupVoteOptionRepository.findOptionsWithCandidateByVoteId(vote.getGroupVoteId()))
                .thenReturn(List.of(agreeOption, disagreeOption));
        when(groupVoteBallotRepository.countByGroupVoteAndGroupVoteOption(vote, agreeOption)).thenReturn(0L);
        when(groupVoteBallotRepository.countByGroupVoteAndGroupVoteOption(vote, disagreeOption)).thenReturn(2L);
        when(groupVoteRepository.save(vote)).thenReturn(vote);
        when(matchingMemberRepository.findActiveMembers(groupId, JoinStatus.ACCEPTED))
                .thenReturn(List.of(leaderMember, memberEntity));

        GroupVoteResponse response = groupVoteService.closeVote(groupId, vote.getGroupVoteId(), leaderUser.getUserId());

        assertThat(response.getWinningOptionId()).isEqualTo(disagreeOption.getGroupVoteOptionId());
        assertThat(group.getStatus()).isEqualTo(MatchingGroupStatus.OPEN);
        verify(matchingGroupRepository, never()).save(any());
        verify(groupTripRepository, never()).save(any());
    }

    @Test
    @DisplayName("closeVote trên GROUP_DISSOLUTION: đồng hạng -> không side effect, group không đổi")
    void closeVote_DissolutionTie_NoSideEffect() {
        GroupVote vote = newOpenVote(2, LocalDateTime.now().minusMinutes(1));
        vote.setVoteType(VoteType.GROUP_DISSOLUTION);
        group.setStatus(MatchingGroupStatus.OPEN);
        GroupVoteOption agreeOption = newOption(vote, 1, "Đồng ý");
        GroupVoteOption disagreeOption = newOption(vote, 2, "Không đồng ý");

        when(matchingMemberRepository.findByGroupIdAndUserId(groupId, leaderUser.getUserId()))
                .thenReturn(Optional.of(leaderMember));
        when(groupVoteRepository.findByIdForUpdate(vote.getGroupVoteId())).thenReturn(Optional.of(vote));
        when(groupVoteBallotRepository.countByGroupVote(vote)).thenReturn(2L);
        when(groupVoteOptionRepository.findOptionsWithCandidateByVoteId(vote.getGroupVoteId()))
                .thenReturn(List.of(agreeOption, disagreeOption));
        when(groupVoteBallotRepository.countByGroupVoteAndGroupVoteOption(vote, agreeOption)).thenReturn(1L);
        when(groupVoteBallotRepository.countByGroupVoteAndGroupVoteOption(vote, disagreeOption)).thenReturn(1L);
        when(groupVoteRepository.save(vote)).thenReturn(vote);
        when(matchingMemberRepository.findActiveMembers(groupId, JoinStatus.ACCEPTED))
                .thenReturn(List.of(leaderMember, memberEntity));

        GroupVoteResponse response = groupVoteService.closeVote(groupId, vote.getGroupVoteId(), leaderUser.getUserId());

        assertThat(response.getWinningOptionId()).isNull();
        assertThat(group.getStatus()).isEqualTo(MatchingGroupStatus.OPEN);
        verify(matchingGroupRepository, never()).save(any());
    }

    private GroupVoteBallot newBallot(GroupVote vote, GroupVoteOption option, MatchingMember voter) {
        GroupVoteBallot ballot = new GroupVoteBallot();
        ballot.setGroupVote(vote);
        ballot.setGroupVoteOption(option);
        ballot.setVoterMatchingMember(voter);
        return ballot;
    }

    @Test
    @DisplayName("handleMemberEligibilityLoss: member không có ballot ở vote OPEN nào -> không đụng tới vote nào")
    void handleMemberEligibilityLoss_NoBallots_NoOp() {
        when(groupVoteBallotRepository.findByVoterMatchingMemberAndGroupVote_StatusAndGroupVote_IsDeletedFalse(
                memberEntity, VoteStatus.OPEN)).thenReturn(List.of());

        groupVoteService.handleMemberEligibilityLoss(memberEntity);

        verify(groupVoteRepository, never()).findByIdForUpdate(any());
        verify(groupVoteBallotRepository, never()).deleteByGroupVoteAndVoterMatchingMember(any(), any());
        verify(groupVoteRepository, never()).save(any());
    }

    @Test
    @DisplayName("handleMemberEligibilityLoss: member có ballot, vote chưa đủ điều kiện đóng -> xoá ballot, giảm eligibleVoterCount, vote vẫn OPEN")
    void handleMemberEligibilityLoss_HasBallot_NotReadyToClose_DecrementsOnly() {
        GroupVote vote = newOpenVote(3, LocalDateTime.now().plusDays(1));
        GroupVoteOption optionA = newOption(vote, 1, "Quán A");
        GroupVoteBallot ballot = newBallot(vote, optionA, memberEntity);

        when(groupVoteBallotRepository.findByVoterMatchingMemberAndGroupVote_StatusAndGroupVote_IsDeletedFalse(
                memberEntity, VoteStatus.OPEN)).thenReturn(List.of(ballot));
        when(groupVoteRepository.findByIdForUpdate(vote.getGroupVoteId())).thenReturn(Optional.of(vote));
        when(groupVoteRepository.save(vote)).thenReturn(vote);
        // Sau khi xoá ballot của member này, chỉ còn 0 phiếu trong khi eligibleVoterCount giảm còn 2 -> chưa đủ.
        when(groupVoteBallotRepository.countByGroupVote(vote)).thenReturn(0L);

        groupVoteService.handleMemberEligibilityLoss(memberEntity);

        verify(groupVoteBallotRepository).deleteByGroupVoteAndVoterMatchingMember(vote, memberEntity);
        assertThat(vote.getEligibleVoterCount()).isEqualTo(2);
        assertThat(vote.getStatus()).isEqualTo(VoteStatus.OPEN);
    }

    @Test
    @DisplayName("handleMemberEligibilityLoss: sau khi giảm, phiếu còn lại đã đủ eligibleVoterCount mới -> tự đóng ngay")
    void handleMemberEligibilityLoss_HasBallot_RemainingAllVoted_AutoCloses() {
        GroupVote vote = newOpenVote(2, LocalDateTime.now().plusDays(1));
        GroupVoteOption optionA = newOption(vote, 1, "Quán A");
        GroupVoteOption optionB = newOption(vote, 2, "Quán B");
        GroupVoteBallot ballot = newBallot(vote, optionA, otherMemberEntity);

        when(groupVoteBallotRepository.findByVoterMatchingMemberAndGroupVote_StatusAndGroupVote_IsDeletedFalse(
                otherMemberEntity, VoteStatus.OPEN)).thenReturn(List.of(ballot));
        when(groupVoteRepository.findByIdForUpdate(vote.getGroupVoteId())).thenReturn(Optional.of(vote));
        when(groupVoteRepository.save(vote)).thenReturn(vote);
        // eligibleVoterCount giảm 2 -> 1; 1 phiếu còn lại (memberEntity) vẫn đủ -> tự đóng.
        when(groupVoteBallotRepository.countByGroupVote(vote)).thenReturn(1L);
        when(groupVoteOptionRepository.findOptionsWithCandidateByVoteId(vote.getGroupVoteId()))
                .thenReturn(List.of(optionA, optionB));
        when(groupVoteBallotRepository.countByGroupVoteAndGroupVoteOption(vote, optionA)).thenReturn(1L);
        when(groupVoteBallotRepository.countByGroupVoteAndGroupVoteOption(vote, optionB)).thenReturn(0L);
        when(matchingMemberRepository.findActiveMembers(groupId, JoinStatus.ACCEPTED))
                .thenReturn(List.of(leaderMember, memberEntity));

        groupVoteService.handleMemberEligibilityLoss(otherMemberEntity);

        assertThat(vote.getEligibleVoterCount()).isEqualTo(1);
        assertThat(vote.getStatus()).isEqualTo(VoteStatus.CLOSED);
        assertThat(vote.getWinningOption()).isEqualTo(optionA);
    }

    @Test
    @DisplayName("handleMemberEligibilityLoss: vote đã bị đóng bởi request khác trong lúc chờ lock -> bỏ qua, không lỗi")
    void handleMemberEligibilityLoss_VoteAlreadyClosedByRace_SkipsSafely() {
        GroupVote vote = newOpenVote(3, LocalDateTime.now().plusDays(1));
        GroupVoteOption optionA = newOption(vote, 1, "Quán A");
        GroupVoteBallot ballot = newBallot(vote, optionA, memberEntity);
        GroupVote lockedVote = newOpenVote(3, LocalDateTime.now().plusDays(1));
        lockedVote.setGroupVoteId(vote.getGroupVoteId());
        lockedVote.setStatus(VoteStatus.CLOSED);

        when(groupVoteBallotRepository.findByVoterMatchingMemberAndGroupVote_StatusAndGroupVote_IsDeletedFalse(
                memberEntity, VoteStatus.OPEN)).thenReturn(List.of(ballot));
        when(groupVoteRepository.findByIdForUpdate(vote.getGroupVoteId())).thenReturn(Optional.of(lockedVote));

        groupVoteService.handleMemberEligibilityLoss(memberEntity);

        verify(groupVoteBallotRepository, never()).deleteByGroupVoteAndVoterMatchingMember(any(), any());
        verify(groupVoteRepository, never()).save(any());
    }

    @Test
    @DisplayName("Xác nhận: eligibleVoterCount là snapshot tại thời điểm mở vote — castBallot/closeVote không tính lại theo số member ACCEPTED hiện tại")
    void eligibleVoterCount_IsSnapshotAtOpenTime_NotRecalculatedOnCastOrClose() {
        GroupVote vote = newOpenVote(2, LocalDateTime.now().plusDays(1));
        GroupVoteOption optionA = newOption(vote, 1, "Quán A");
        GroupVoteOption optionB = newOption(vote, 2, "Quán B");

        // Group thực tế đã có 3 member ACCEPTED (1 người mới join sau khi vote mở) nhưng
        // eligibleVoterCount trên vote vẫn giữ nguyên = 2 vì là snapshot lúc mở.
        when(matchingMemberRepository.findByGroupIdAndUserId(groupId, memberUser.getUserId()))
                .thenReturn(Optional.of(memberEntity));
        when(groupVoteRepository.findByIdForUpdate(vote.getGroupVoteId())).thenReturn(Optional.of(vote));
        when(groupVoteOptionRepository.findByGroupVoteOptionIdAndGroupVote_GroupVoteIdAndIsDeletedFalse(
                optionA.getGroupVoteOptionId(), vote.getGroupVoteId())).thenReturn(Optional.of(optionA));
        when(groupVoteBallotRepository.countByGroupVote(vote)).thenReturn(1L);

        CastBallotRequest request = CastBallotRequest.builder().optionId(optionA.getGroupVoteOptionId()).build();
        groupVoteService.castBallot(groupId, vote.getGroupVoteId(), request, memberUser.getUserId());

        assertThat(vote.getEligibleVoterCount()).isEqualTo(2);
        verify(matchingMemberRepository, never()).countActiveMembersByGroupIdAndStatus(any(), any());
    }

    @Test
    @DisplayName("[P5-S4 regression] castBallot trên poll OTHER: outsider (không phải member) -> NOT_ACCEPTED_MATCHING_MEMBER")
    void castBallot_OtherPoll_OutsiderNotMember_ThrowsError() {
        UUID outsiderUserId = UUID.randomUUID();
        GroupVote vote = newOpenVote(3, LocalDateTime.now().plusDays(1));

        when(matchingMemberRepository.findByGroupIdAndUserId(groupId, outsiderUserId))
                .thenReturn(Optional.empty());

        CastBallotRequest request = CastBallotRequest.builder().optionId(UUID.randomUUID()).build();

        assertThatThrownBy(() -> groupVoteService.castBallot(groupId, vote.getGroupVoteId(), request, outsiderUserId))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_ACCEPTED_MATCHING_MEMBER);

        verify(groupVoteRepository, never()).findByIdForUpdate(any());
        verify(groupVoteBallotRepository, never()).save(any());
    }

    @Test
    @DisplayName("[P5-S4 regression] closeVote/cancelVote trên poll OTHER: outsider (không phải member) bị chặn")
    void closeVoteAndCancelVote_OtherPoll_OutsiderNotMember_ThrowsError() {
        UUID outsiderUserId = UUID.randomUUID();
        GroupVote vote = newOpenVote(3, LocalDateTime.now().plusDays(1));

        when(matchingMemberRepository.findByGroupIdAndUserId(groupId, outsiderUserId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> groupVoteService.closeVote(groupId, vote.getGroupVoteId(), outsiderUserId))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_ACCEPTED_MATCHING_MEMBER);
        assertThatThrownBy(() -> groupVoteService.cancelVote(groupId, vote.getGroupVoteId(), outsiderUserId))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_ACCEPTED_MATCHING_MEMBER);

        verify(groupVoteRepository, never()).findByIdForUpdate(any());
        verify(groupVoteRepository, never()).save(any());
    }

    @Test
    @DisplayName("[P5-S4 regression] closeVote trên poll OTHER có winner: không bao giờ đổi Leader hay huỷ nhóm (side effect chỉ dành cho LEADER_ELECTION/GROUP_DISSOLUTION)")
    void closeVote_OtherPollWithWinner_NeverAppliesElectionOrDissolutionSideEffect() {
        GroupVote vote = newOpenVote(2, LocalDateTime.now().minusMinutes(1));
        // VoteType.OTHER (mặc định của newOpenVote) — không phải LEADER_ELECTION/GROUP_DISSOLUTION.
        GroupVoteOption optionA = newOption(vote, 1, "Quán A");
        GroupVoteOption optionB = newOption(vote, 2, "Quán B");

        when(matchingMemberRepository.findByGroupIdAndUserId(groupId, leaderUser.getUserId()))
                .thenReturn(Optional.of(leaderMember));
        when(groupVoteRepository.findByIdForUpdate(vote.getGroupVoteId())).thenReturn(Optional.of(vote));
        when(groupVoteBallotRepository.countByGroupVote(vote)).thenReturn(2L);
        when(groupVoteOptionRepository.findOptionsWithCandidateByVoteId(vote.getGroupVoteId()))
                .thenReturn(List.of(optionA, optionB));
        when(groupVoteBallotRepository.countByGroupVoteAndGroupVoteOption(vote, optionA)).thenReturn(2L);
        when(groupVoteBallotRepository.countByGroupVoteAndGroupVoteOption(vote, optionB)).thenReturn(0L);
        when(groupVoteRepository.save(vote)).thenReturn(vote);
        when(matchingMemberRepository.findActiveMembers(groupId, JoinStatus.ACCEPTED))
                .thenReturn(List.of(leaderMember, memberEntity));

        GroupVoteResponse response = groupVoteService.closeVote(groupId, vote.getGroupVoteId(), leaderUser.getUserId());

        assertThat(vote.getVoteType()).isEqualTo(VoteType.OTHER);
        assertThat(response.getWinningOptionId()).isEqualTo(optionA.getGroupVoteOptionId());
        assertThat(leaderMember.getRole()).isEqualTo(MatchingRole.LEADER);
        assertThat(group.getStatus()).isNotEqualTo(MatchingGroupStatus.CANCELLED);
        verify(matchingGroupRepository, never()).save(any());
        verify(matchingMemberRepository, never()).save(any());
        verify(groupTripRepository, never()).save(any());
    }

    @Test
    @DisplayName("[P5-S4 regression] getVotes(voteType=OTHER): uỷ quyền đúng filter xuống repository, không tự lọc lại ở service")
    void getVotes_FilterByVoteTypeOther_DelegatesToRepositoryWithSameFilter() {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 10);
        when(matchingMemberRepository.findByGroupIdAndUserId(groupId, leaderUser.getUserId()))
                .thenReturn(Optional.of(leaderMember));
        when(groupVoteRepository.findByGroupWithFilters(groupId, VoteType.OTHER, null, pageable))
                .thenReturn(org.springframework.data.domain.Page.empty());

        groupVoteService.getVotes(groupId, VoteType.OTHER, null, pageable, leaderUser.getUserId());

        verify(groupVoteRepository).findByGroupWithFilters(groupId, VoteType.OTHER, null, pageable);
    }
}
