package com.sep.treksphere.matching.service;

import com.sep.treksphere.common.exception.AppException;
import com.sep.treksphere.common.exception.ErrorCode;
import com.sep.treksphere.matching.dto.request.CastBallotRequest;
import com.sep.treksphere.matching.dto.request.CreateGroupVoteRequest;
import com.sep.treksphere.matching.dto.response.GroupVoteResponse;
import com.sep.treksphere.matching.entity.GroupVote;
import com.sep.treksphere.matching.entity.GroupVoteBallot;
import com.sep.treksphere.matching.entity.GroupVoteOption;
import com.sep.treksphere.matching.entity.MatchingGroup;
import com.sep.treksphere.matching.entity.MatchingMember;
import com.sep.treksphere.matching.enums.JoinStatus;
import com.sep.treksphere.matching.enums.MatchingRole;
import com.sep.treksphere.matching.enums.VoteStatus;
import com.sep.treksphere.matching.enums.VoteType;
import com.sep.treksphere.matching.repository.GroupVoteBallotRepository;
import com.sep.treksphere.matching.repository.GroupVoteOptionRepository;
import com.sep.treksphere.matching.repository.GroupVoteRepository;
import com.sep.treksphere.matching.repository.MatchingMemberRepository;
import com.sep.treksphere.matching.service.impl.GroupVoteServiceImpl;
import com.sep.treksphere.notification.NotificationService;
import com.sep.treksphere.user.User;
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
    private NotificationService notificationService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

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
        when(groupVoteBallotRepository.existsByGroupVoteAndVoterMatchingMember(vote, memberEntity)).thenReturn(false);
        when(groupVoteOptionRepository.findByGroupVoteOptionIdAndGroupVote_GroupVoteIdAndIsDeletedFalse(
                foreignOptionId, vote.getGroupVoteId())).thenReturn(Optional.empty());

        CastBallotRequest request = CastBallotRequest.builder().optionId(foreignOptionId).build();

        assertThatThrownBy(() -> groupVoteService.castBallot(groupId, vote.getGroupVoteId(), request, memberUser.getUserId()))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.GROUP_VOTE_INVALID_OPTION);

        verify(groupVoteBallotRepository, never()).save(any());
    }

    @Test
    @DisplayName("castBallot: đã bỏ phiếu rồi -> GROUP_VOTE_ALREADY_VOTED")
    void castBallot_AlreadyVoted_ThrowsError() {
        GroupVote vote = newOpenVote(3, LocalDateTime.now().plusDays(1));

        when(matchingMemberRepository.findByGroupIdAndUserId(groupId, memberUser.getUserId()))
                .thenReturn(Optional.of(memberEntity));
        when(groupVoteRepository.findByIdForUpdate(vote.getGroupVoteId())).thenReturn(Optional.of(vote));
        when(groupVoteBallotRepository.existsByGroupVoteAndVoterMatchingMember(vote, memberEntity)).thenReturn(true);

        CastBallotRequest request = CastBallotRequest.builder().optionId(UUID.randomUUID()).build();

        assertThatThrownBy(() -> groupVoteService.castBallot(groupId, vote.getGroupVoteId(), request, memberUser.getUserId()))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.GROUP_VOTE_ALREADY_VOTED);

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
        when(groupVoteOptionRepository.findByGroupVote_GroupVoteIdAndIsDeletedFalseOrderByOptionOrderAsc(vote.getGroupVoteId()))
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
        when(groupVoteBallotRepository.existsByGroupVoteAndVoterMatchingMember(vote, otherMemberEntity)).thenReturn(false);
        when(groupVoteOptionRepository.findByGroupVoteOptionIdAndGroupVote_GroupVoteIdAndIsDeletedFalse(
                optionA.getGroupVoteOptionId(), vote.getGroupVoteId())).thenReturn(Optional.of(optionA));
        // Trước ballot cuối: đã có 2/3 phiếu (A=2). Sau khi lưu ballot thứ 3 (A) -> count = 3 = eligible -> auto-close.
        when(groupVoteBallotRepository.countByGroupVote(vote)).thenReturn(3L);
        when(groupVoteOptionRepository.findByGroupVote_GroupVoteIdAndIsDeletedFalseOrderByOptionOrderAsc(vote.getGroupVoteId()))
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
        when(groupVoteOptionRepository.findByGroupVote_GroupVoteIdAndIsDeletedFalseOrderByOptionOrderAsc(vote.getGroupVoteId()))
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
        when(groupVoteOptionRepository.findByGroupVote_GroupVoteIdAndIsDeletedFalseOrderByOptionOrderAsc(vote.getGroupVoteId()))
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
        when(groupVoteOptionRepository.findByGroupVote_GroupVoteIdAndIsDeletedFalseOrderByOptionOrderAsc(vote.getGroupVoteId()))
                .thenReturn(List.of());
        when(matchingMemberRepository.findActiveMembers(groupId, JoinStatus.ACCEPTED))
                .thenReturn(List.of(leaderMember, memberEntity));

        GroupVoteResponse response = groupVoteService.cancelVote(groupId, vote.getGroupVoteId(), leaderUser.getUserId());

        assertThat(response.getStatus()).isEqualTo(VoteStatus.CLOSED);
        assertThat(response.getWinningOptionId()).isNull();
    }
}
