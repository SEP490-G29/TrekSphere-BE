package com.sep.treksphere.matching.service.impl;

import com.sep.treksphere.common.exception.AppException;
import com.sep.treksphere.common.exception.ErrorCode;
import com.sep.treksphere.matching.dto.request.CastBallotRequest;
import com.sep.treksphere.matching.dto.request.CreateGroupVoteRequest;
import com.sep.treksphere.matching.dto.request.OpenDissolutionVoteRequest;
import com.sep.treksphere.matching.dto.request.OpenLeaderElectionRequest;
import com.sep.treksphere.matching.dto.response.GroupVoteOptionResponse;
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
import com.sep.treksphere.matching.service.GroupVoteService;
import com.sep.treksphere.notification.NotificationEventType;
import com.sep.treksphere.notification.NotificationService;
import com.sep.treksphere.notification.ReferenceType;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GroupVoteServiceImpl implements GroupVoteService {

    private final GroupVoteRepository groupVoteRepository;
    private final GroupVoteOptionRepository groupVoteOptionRepository;
    private final GroupVoteBallotRepository groupVoteBallotRepository;
    private final MatchingMemberRepository matchingMemberRepository;
    private final MatchingGroupRepository matchingGroupRepository;
    private final GroupTripRepository groupTripRepository;
    private final NotificationService notificationService;
    private final SimpMessagingTemplate messagingTemplate;

    private static final String DISSOLUTION_AGREE_LABEL = "Đồng ý";
    private static final String DISSOLUTION_DISAGREE_LABEL = "Không đồng ý";
    private static final int DISSOLUTION_AGREE_ORDER = 1;

    @Override
    @Transactional
    public GroupVoteResponse createGeneralPoll(UUID groupId, CreateGroupVoteRequest request, UUID currentUserId) {
        MatchingMember creator = requireActiveMember(groupId, currentUserId);

        if (groupVoteRepository.existsByMatchingGroup_MatchingGroupIdAndVoteTypeAndStatusAndIsDeletedFalse(
                groupId, VoteType.OTHER, VoteStatus.OPEN)) {
            throw new AppException(ErrorCode.GROUP_VOTE_DUPLICATE_OPEN_TYPE);
        }

        GroupVote vote = new GroupVote();
        vote.setMatchingGroup(creator.getMatchingGroup());
        vote.setVoteType(VoteType.OTHER);
        vote.setTitle(request.getTitle());
        vote.setReason(request.getReason());
        vote.setCreatedByMember(creator);
        vote.setStatus(VoteStatus.OPEN);
        vote.setOpensAt(LocalDateTime.now());
        vote.setClosesAt(request.getClosesAt());
        vote.setEligibleVoterCount(Math.toIntExact(
                matchingMemberRepository.countActiveMembersByGroupIdAndStatus(groupId, JoinStatus.ACCEPTED)));

        GroupVote savedVote;
        try {
            savedVote = groupVoteRepository.save(vote);
        } catch (DataIntegrityViolationException e) {
            throw new AppException(ErrorCode.GROUP_VOTE_DUPLICATE_OPEN_TYPE);
        }

        List<GroupVoteOption> options = new ArrayList<>();
        int order = 1;
        for (String label : request.getOptionLabels()) {
            GroupVoteOption option = new GroupVoteOption();
            option.setGroupVote(savedVote);
            option.setOptionOrder(order++);
            option.setOptionLabel(label);
            options.add(groupVoteOptionRepository.save(option));
        }

        notificationService.notify(activeMemberUserIdsExcept(groupId, currentUserId),
                NotificationEventType.GROUP_VOTE_OPENED,
                ReferenceType.GROUP_VOTE, savedVote.getGroupVoteId(),
                "/trekker/my-groups/" + groupId,
                creator.getUser().getFullName(), savedVote.getTitle());

        GroupVoteResponse response = toResponse(savedVote, options, currentUserId);
        broadcastAfterCommit(groupId, response);
        return response;
    }

    @Override
    @Transactional
    public GroupVoteResponse openLeaderElectionVote(
            UUID groupId, OpenLeaderElectionRequest request, UUID currentUserId) {
        MatchingMember opener = requireActiveMember(groupId, currentUserId);

        if (groupVoteRepository.existsByMatchingGroup_MatchingGroupIdAndVoteTypeAndStatusAndIsDeletedFalse(
                groupId, VoteType.LEADER_ELECTION, VoteStatus.OPEN)) {
            throw new AppException(ErrorCode.GROUP_VOTE_DUPLICATE_OPEN_TYPE);
        }

        List<UUID> distinctCandidateIds = request.getCandidateMemberIds().stream().distinct().toList();
        if (distinctCandidateIds.size() < 2) {
            throw new AppException(ErrorCode.GROUP_VOTE_INVALID_OPTION_COUNT);
        }

        List<MatchingMember> candidates = new ArrayList<>();
        for (UUID candidateId : distinctCandidateIds) {
            MatchingMember candidate = matchingMemberRepository.findMemberByIdAndGroupId(candidateId, groupId)
                    .filter(m -> m.getStatus() == JoinStatus.ACCEPTED)
                    .orElseThrow(() -> new AppException(ErrorCode.GROUP_VOTE_INVALID_CANDIDATE));
            if (candidate.getRole() == MatchingRole.LEADER) {
                throw new AppException(ErrorCode.GROUP_VOTE_INVALID_CANDIDATE);
            }
            candidates.add(candidate);
        }

        GroupVote vote = new GroupVote();
        vote.setMatchingGroup(opener.getMatchingGroup());
        vote.setVoteType(VoteType.LEADER_ELECTION);
        vote.setTitle("Bầu Trưởng nhóm mới");
        vote.setReason(request.getReason());
        vote.setCreatedByMember(opener);
        vote.setStatus(VoteStatus.OPEN);
        vote.setOpensAt(LocalDateTime.now());
        vote.setClosesAt(request.getClosesAt());
        vote.setEligibleVoterCount(Math.toIntExact(
                matchingMemberRepository.countActiveMembersByGroupIdAndStatus(groupId, JoinStatus.ACCEPTED)));

        GroupVote savedVote;
        try {
            savedVote = groupVoteRepository.save(vote);
        } catch (DataIntegrityViolationException e) {
            throw new AppException(ErrorCode.GROUP_VOTE_DUPLICATE_OPEN_TYPE);
        }

        List<GroupVoteOption> options = new ArrayList<>();
        int order = 1;
        for (MatchingMember candidate : candidates) {
            GroupVoteOption option = new GroupVoteOption();
            option.setGroupVote(savedVote);
            option.setOptionOrder(order++);
            option.setOptionLabel(candidate.getUser().getFullName());
            option.setCandidateMatchingMember(candidate);
            options.add(groupVoteOptionRepository.save(option));
        }

        notificationService.notify(activeMemberUserIdsExcept(groupId, currentUserId),
                NotificationEventType.GROUP_VOTE_OPENED,
                ReferenceType.GROUP_VOTE, savedVote.getGroupVoteId(),
                "/trekker/my-groups/" + groupId,
                opener.getUser().getFullName(), savedVote.getTitle());

        GroupVoteResponse response = toResponse(savedVote, options, currentUserId);
        broadcastAfterCommit(groupId, response);
        return response;
    }

    @Override
    @Transactional
    public GroupVoteResponse openDissolutionVote(
            UUID groupId, OpenDissolutionVoteRequest request, UUID currentUserId) {
        MatchingMember opener = requireActiveMember(groupId, currentUserId);

        if (groupVoteRepository.existsByMatchingGroup_MatchingGroupIdAndVoteTypeAndStatusAndIsDeletedFalse(
                groupId, VoteType.GROUP_DISSOLUTION, VoteStatus.OPEN)) {
            throw new AppException(ErrorCode.GROUP_VOTE_DUPLICATE_OPEN_TYPE);
        }

        GroupVote vote = new GroupVote();
        vote.setMatchingGroup(opener.getMatchingGroup());
        vote.setVoteType(VoteType.GROUP_DISSOLUTION);
        vote.setTitle("Biểu quyết giải tán nhóm");
        vote.setReason(request.getReason());
        vote.setCreatedByMember(opener);
        vote.setStatus(VoteStatus.OPEN);
        vote.setOpensAt(LocalDateTime.now());
        vote.setClosesAt(request.getClosesAt());
        vote.setEligibleVoterCount(Math.toIntExact(
                matchingMemberRepository.countActiveMembersByGroupIdAndStatus(groupId, JoinStatus.ACCEPTED)));

        GroupVote savedVote;
        try {
            savedVote = groupVoteRepository.save(vote);
        } catch (DataIntegrityViolationException e) {
            throw new AppException(ErrorCode.GROUP_VOTE_DUPLICATE_OPEN_TYPE);
        }

        GroupVoteOption agreeOption = new GroupVoteOption();
        agreeOption.setGroupVote(savedVote);
        agreeOption.setOptionOrder(DISSOLUTION_AGREE_ORDER);
        agreeOption.setOptionLabel(DISSOLUTION_AGREE_LABEL);
        GroupVoteOption savedAgree = groupVoteOptionRepository.save(agreeOption);

        GroupVoteOption disagreeOption = new GroupVoteOption();
        disagreeOption.setGroupVote(savedVote);
        disagreeOption.setOptionOrder(DISSOLUTION_AGREE_ORDER + 1);
        disagreeOption.setOptionLabel(DISSOLUTION_DISAGREE_LABEL);
        GroupVoteOption savedDisagree = groupVoteOptionRepository.save(disagreeOption);

        List<GroupVoteOption> options = List.of(savedAgree, savedDisagree);

        notificationService.notify(activeMemberUserIdsExcept(groupId, currentUserId),
                NotificationEventType.GROUP_VOTE_OPENED,
                ReferenceType.GROUP_VOTE, savedVote.getGroupVoteId(),
                "/trekker/my-groups/" + groupId,
                opener.getUser().getFullName(), savedVote.getTitle());

        GroupVoteResponse response = toResponse(savedVote, options, currentUserId);
        broadcastAfterCommit(groupId, response);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<GroupVoteResponse> getVotes(
            UUID groupId, VoteType voteType, VoteStatus status, Pageable pageable, UUID currentUserId) {
        requireActiveMember(groupId, currentUserId);
        return groupVoteRepository.findByGroupWithFilters(groupId, voteType, status, pageable)
                .map(vote -> toResponse(vote, loadOptions(vote.getGroupVoteId()), currentUserId));
    }

    @Override
    @Transactional(readOnly = true)
    public GroupVoteResponse getVoteDetail(UUID groupId, UUID voteId, UUID currentUserId) {
        requireActiveMember(groupId, currentUserId);
        GroupVote vote = groupVoteRepository.findByGroupIdAndVoteId(groupId, voteId)
                .orElseThrow(() -> new AppException(ErrorCode.GROUP_VOTE_NOT_FOUND));
        return toResponse(vote, loadOptions(voteId), currentUserId);
    }

    @Override
    @Transactional
    public GroupVoteResponse castBallot(UUID groupId, UUID voteId, CastBallotRequest request, UUID currentUserId) {
        MatchingMember voter = requireActiveMember(groupId, currentUserId);
        GroupVote vote = lockVoteInGroupOrThrow(groupId, voteId);

        if (vote.getStatus() != VoteStatus.OPEN) {
            throw new AppException(ErrorCode.GROUP_VOTE_CLOSED);
        }
        if (isDeadlineReached(vote)) {
            return doClose(vote, currentUserId);
        }
        if (groupVoteBallotRepository.existsByGroupVoteAndVoterMatchingMember(vote, voter)) {
            throw new AppException(ErrorCode.GROUP_VOTE_ALREADY_VOTED);
        }

        GroupVoteOption option = groupVoteOptionRepository
                .findByGroupVoteOptionIdAndGroupVote_GroupVoteIdAndIsDeletedFalse(request.getOptionId(), voteId)
                .orElseThrow(() -> new AppException(ErrorCode.GROUP_VOTE_INVALID_OPTION));

        GroupVoteBallot ballot = new GroupVoteBallot();
        ballot.setGroupVote(vote);
        ballot.setGroupVoteOption(option);
        ballot.setVoterMatchingMember(voter);
        groupVoteBallotRepository.save(ballot);

        long ballotCount = groupVoteBallotRepository.countByGroupVote(vote);
        if (ballotCount >= vote.getEligibleVoterCount()) {
            return doClose(vote, currentUserId);
        }

        GroupVoteResponse response = toResponse(vote, loadOptions(voteId), currentUserId);
        broadcastAfterCommit(groupId, response);
        return response;
    }

    @Override
    @Transactional
    public GroupVoteResponse closeVote(UUID groupId, UUID voteId, UUID currentUserId) {
        requireActiveMember(groupId, currentUserId);
        GroupVote vote = lockVoteInGroupOrThrow(groupId, voteId);

        if (vote.getStatus() != VoteStatus.OPEN) {
            // Retry close phải idempotent theo Artifact A — trả lại kết quả hiện tại, không lỗi.
            return toResponse(vote, loadOptions(voteId), currentUserId);
        }

        long ballotCount = groupVoteBallotRepository.countByGroupVote(vote);
        boolean allVoted = ballotCount >= vote.getEligibleVoterCount();
        if (!isDeadlineReached(vote) && !allVoted) {
            throw new AppException(ErrorCode.GROUP_VOTE_NOT_READY_TO_CLOSE);
        }

        return doClose(vote, currentUserId);
    }

    @Override
    @Transactional
    public GroupVoteResponse cancelVote(UUID groupId, UUID voteId, UUID currentUserId) {
        MatchingMember actor = requireActiveMember(groupId, currentUserId);
        GroupVote vote = lockVoteInGroupOrThrow(groupId, voteId);

        if (vote.getStatus() != VoteStatus.OPEN) {
            throw new AppException(ErrorCode.GROUP_VOTE_CLOSED);
        }

        boolean isOpener = vote.getCreatedByMember().getMatchingMemberId().equals(actor.getMatchingMemberId());
        boolean isLeader = actor.getRole() == MatchingRole.LEADER;
        if (!isOpener && !isLeader) {
            throw new AppException(ErrorCode.MATCHING_GROUP_UNAUTHORIZED_MANAGE);
        }

        vote.setStatus(VoteStatus.CLOSED);
        vote.setClosedAt(LocalDateTime.now());
        vote.setWinningOption(null);
        GroupVote savedVote = groupVoteRepository.save(vote);

        notificationService.notify(activeMemberUserIdsExcept(groupId, currentUserId),
                NotificationEventType.GROUP_VOTE_CLOSED,
                ReferenceType.GROUP_VOTE, savedVote.getGroupVoteId(),
                "/trekker/my-groups/" + groupId, savedVote.getTitle());

        GroupVoteResponse response = toResponse(savedVote, loadOptions(voteId), currentUserId);
        broadcastAfterCommit(groupId, response);
        return response;
    }

    @Override
    @Transactional
    public void handleMemberEligibilityLoss(MatchingMember member) {
        List<GroupVoteBallot> ballots = groupVoteBallotRepository
                .findByVoterMatchingMemberAndGroupVote_StatusAndGroupVote_IsDeletedFalse(member, VoteStatus.OPEN);

        for (GroupVoteBallot ballot : ballots) {
            GroupVote vote = groupVoteRepository.findByIdForUpdate(ballot.getGroupVote().getGroupVoteId())
                    .orElse(null);
            if (vote == null || vote.getStatus() != VoteStatus.OPEN) {
                // Đã đóng bởi request khác trong lúc chờ lock — bỏ qua, không còn gì để sửa.
                continue;
            }

            groupVoteBallotRepository.deleteByGroupVoteAndVoterMatchingMember(vote, member);
            vote.setEligibleVoterCount(vote.getEligibleVoterCount() - 1);
            groupVoteRepository.save(vote);

            long remainingBallotCount = groupVoteBallotRepository.countByGroupVote(vote);
            if (isDeadlineReached(vote) || remainingBallotCount >= vote.getEligibleVoterCount()) {
                doClose(vote, null);
            }
        }
    }

    /**
     * Tally phiếu, xác định winner (plurality, tie/no-ballot = null), áp side effect theo
     * voteType nếu có winner, lưu + notify + broadcast. Dùng chung cho auto-close (đủ phiếu/
     * hết hạn khi cast) và close/deadline thủ công.
     */
    private GroupVoteResponse doClose(GroupVote vote, UUID actorUserId) {
        List<GroupVoteOption> options = loadOptions(vote.getGroupVoteId());

        Map<UUID, Long> tally = options.stream()
                .collect(Collectors.toMap(GroupVoteOption::getGroupVoteOptionId,
                        o -> groupVoteBallotRepository.countByGroupVoteAndGroupVoteOption(vote, o)));

        GroupVoteOption winner = determineWinner(options, tally);

        vote.setStatus(VoteStatus.CLOSED);
        vote.setClosedAt(LocalDateTime.now());
        vote.setWinningOption(winner);

        if (winner != null) {
            applyWinnerSideEffect(vote, winner);
        }

        GroupVote savedVote = groupVoteRepository.save(vote);
        UUID groupId = savedVote.getMatchingGroup().getMatchingGroupId();

        notificationService.notify(activeMemberUserIdsExcept(groupId, actorUserId),
                NotificationEventType.GROUP_VOTE_CLOSED,
                ReferenceType.GROUP_VOTE, savedVote.getGroupVoteId(),
                "/trekker/my-groups/" + groupId, savedVote.getTitle());

        if (winner != null && savedVote.getVoteType() == VoteType.LEADER_ELECTION) {
            notificationService.notify(activeMemberUserIdsExcept(groupId, null),
                    NotificationEventType.GROUP_LEADER_CHANGED,
                    ReferenceType.GROUP_VOTE, savedVote.getGroupVoteId(),
                    "/trekker/my-groups/" + groupId,
                    winner.getCandidateMatchingMember().getUser().getFullName());
        }

        if (winner != null && savedVote.getVoteType() == VoteType.GROUP_DISSOLUTION
                && winner.getOptionOrder() == DISSOLUTION_AGREE_ORDER) {
            notificationService.notify(activeMemberUserIdsExcept(groupId, null),
                    NotificationEventType.MATCHING_GROUP_CANCELLED,
                    ReferenceType.MATCHING_GROUP, groupId,
                    "/trekker/my-groups/" + groupId,
                    savedVote.getMatchingGroup().getGroupName());
        }

        GroupVoteResponse response = toResponse(savedVote, options, actorUserId);
        broadcastAfterCommit(groupId, response);
        return response;
    }

    private GroupVoteOption determineWinner(List<GroupVoteOption> options, Map<UUID, Long> tally) {
        long maxVotes = tally.values().stream().mapToLong(Long::longValue).max().orElse(0);
        if (maxVotes == 0) {
            return null;
        }
        List<GroupVoteOption> topOptions = options.stream()
                .filter(o -> tally.get(o.getGroupVoteOptionId()) == maxVotes)
                .toList();
        return topOptions.size() == 1 ? topOptions.get(0) : null;
    }

    /**
     * OTHER không có side effect (chỉ lưu kết quả — đúng phạm vi P5-S4). LEADER_ELECTION đổi
     * Leader atomic (P4-S3). GROUP_DISSOLUTION huỷ nhóm + trip PLANNED atomic khi "Đồng ý"
     * thắng (P4-S4); nếu "Không đồng ý" thắng thì không side effect, nhóm hoạt động bình thường.
     */
    private void applyWinnerSideEffect(GroupVote vote, GroupVoteOption winner) {
        switch (vote.getVoteType()) {
            case OTHER -> {
                // không side effect
            }
            case LEADER_ELECTION -> applyLeaderElectionSideEffect(vote, winner);
            case GROUP_DISSOLUTION -> applyDissolutionSideEffect(vote, winner);
        }
    }

    /**
     * Đổi Leader cũ -> MEMBER và winner -> LEADER trong cùng transaction đang lock `vote`;
     * partial UQ `uq_group_active_leader` là lưới an toàn DB-level đảm bảo không có 2 Leader
     * active cùng lúc. Không đổi `matchingGroup.owner_id` (chỉ là người tạo nhóm).
     */
    private void applyLeaderElectionSideEffect(GroupVote vote, GroupVoteOption winner) {
        MatchingMember newLeader = winner.getCandidateMatchingMember();
        UUID groupId = vote.getMatchingGroup().getMatchingGroupId();

        matchingMemberRepository.findActiveMembers(groupId, JoinStatus.ACCEPTED).stream()
                .filter(m -> m.getRole() == MatchingRole.LEADER)
                .filter(m -> !m.getMatchingMemberId().equals(newLeader.getMatchingMemberId()))
                .forEach(oldLeader -> {
                    oldLeader.setRole(MatchingRole.MEMBER);
                    matchingMemberRepository.save(oldLeader);
                });

        newLeader.setRole(MatchingRole.LEADER);
        matchingMemberRepository.save(newLeader);
    }

    /**
     * Chỉ áp dụng khi option order 1 ("Đồng ý") thắng — theo đúng thứ tự cố định do
     * {@code openDissolutionVote} tự dựng, không so label. Chuyển group sang CANCELLED và
     * huỷ GroupTrip đang PLANNED (nếu có) trong cùng transaction đang lock `vote`. "Không đồng
     * ý" thắng hoặc tie/no-ballot đều không side effect (nhóm tiếp tục hoạt động bình thường).
     */
    private void applyDissolutionSideEffect(GroupVote vote, GroupVoteOption winner) {
        if (winner.getOptionOrder() != DISSOLUTION_AGREE_ORDER) {
            return;
        }

        MatchingGroup matchingGroup = vote.getMatchingGroup();
        matchingGroup.setStatus(MatchingGroupStatus.CANCELLED);
        matchingGroupRepository.save(matchingGroup);

        groupTripRepository.findByMatchingGroup_MatchingGroupId(matchingGroup.getMatchingGroupId())
                .filter(trip -> trip.getStatus() == GroupTripStatus.PLANNED)
                .ifPresent(trip -> {
                    trip.setStatus(GroupTripStatus.CANCELLED);
                    groupTripRepository.save(trip);
                });
    }

    private boolean isDeadlineReached(GroupVote vote) {
        return vote.getClosesAt() != null && !vote.getClosesAt().isAfter(LocalDateTime.now());
    }

    private MatchingMember requireActiveMember(UUID groupId, UUID userId) {
        return matchingMemberRepository.findByGroupIdAndUserId(groupId, userId)
                .filter(m -> m.getStatus() == JoinStatus.ACCEPTED)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_ACCEPTED_MATCHING_MEMBER));
    }

    private GroupVote lockVoteInGroupOrThrow(UUID groupId, UUID voteId) {
        GroupVote vote = groupVoteRepository.findByIdForUpdate(voteId)
                .orElseThrow(() -> new AppException(ErrorCode.GROUP_VOTE_NOT_FOUND));
        if (!vote.getMatchingGroup().getMatchingGroupId().equals(groupId)) {
            throw new AppException(ErrorCode.GROUP_VOTE_NOT_FOUND);
        }
        return vote;
    }

    private List<GroupVoteOption> loadOptions(UUID voteId) {
        return groupVoteOptionRepository.findByGroupVote_GroupVoteIdAndIsDeletedFalseOrderByOptionOrderAsc(voteId);
    }

    private List<UUID> activeMemberUserIdsExcept(UUID groupId, UUID excludeUserId) {
        return matchingMemberRepository.findActiveMembers(groupId, JoinStatus.ACCEPTED)
                .stream()
                .map(m -> m.getUser().getUserId())
                .filter(id -> excludeUserId == null || !id.equals(excludeUserId))
                .toList();
    }

    private void broadcastAfterCommit(UUID groupId, GroupVoteResponse response) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                messagingTemplate.convertAndSend("/topic/matching-groups/" + groupId + "/votes", response);
            }
        });
    }

    private GroupVoteResponse toResponse(GroupVote vote, List<GroupVoteOption> options, UUID currentUserId) {
        Map<UUID, Long> tally = options.stream()
                .collect(Collectors.toMap(GroupVoteOption::getGroupVoteOptionId,
                        o -> groupVoteBallotRepository.countByGroupVoteAndGroupVoteOption(vote, o)));

        UUID myBallotOptionId = null;
        if (currentUserId != null) {
            Optional<MatchingMember> viewer = matchingMemberRepository.findByGroupIdAndUserId(
                    vote.getMatchingGroup().getMatchingGroupId(), currentUserId);
            if (viewer.isPresent()) {
                myBallotOptionId = groupVoteBallotRepository
                        .findByGroupVoteAndVoterMatchingMember(vote, viewer.get())
                        .map(b -> b.getGroupVoteOption().getGroupVoteOptionId())
                        .orElse(null);
            }
        }

        List<GroupVoteOptionResponse> optionResponses = options.stream()
                .map(o -> GroupVoteOptionResponse.builder()
                        .groupVoteOptionId(o.getGroupVoteOptionId())
                        .optionOrder(o.getOptionOrder())
                        .optionLabel(o.getOptionLabel())
                        .candidateMemberId(o.getCandidateMatchingMember() != null
                                ? o.getCandidateMatchingMember().getMatchingMemberId() : null)
                        .candidateMemberName(o.getCandidateMatchingMember() != null
                                ? o.getCandidateMatchingMember().getUser().getFullName() : null)
                        .ballotCount(tally.getOrDefault(o.getGroupVoteOptionId(), 0L))
                        .build())
                .toList();

        return GroupVoteResponse.builder()
                .groupVoteId(vote.getGroupVoteId())
                .matchingGroupId(vote.getMatchingGroup().getMatchingGroupId())
                .voteType(vote.getVoteType())
                .title(vote.getTitle())
                .reason(vote.getReason())
                .createdByMemberId(vote.getCreatedByMember().getMatchingMemberId())
                .createdByName(vote.getCreatedByMember().getUser().getFullName())
                .status(vote.getStatus())
                .opensAt(vote.getOpensAt())
                .closesAt(vote.getClosesAt())
                .eligibleVoterCount(vote.getEligibleVoterCount())
                .winningOptionId(vote.getWinningOption() != null ? vote.getWinningOption().getGroupVoteOptionId() : null)
                .closedAt(vote.getClosedAt())
                .options(optionResponses)
                .myBallotOptionId(myBallotOptionId)
                .createdAt(vote.getCreatedAt())
                .updatedAt(vote.getUpdatedAt())
                .build();
    }
}
