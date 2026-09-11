package com.sep.treksphere.matching.service.impl;

import com.sep.treksphere.common.constant.MessageConstant;
import com.sep.treksphere.common.exception.AppException;
import com.sep.treksphere.common.exception.ErrorCode;
import com.sep.treksphere.matching.dto.request.GroupSettlementProofRequest;
import com.sep.treksphere.matching.dto.request.GroupSettlementRejectRequest;
import com.sep.treksphere.matching.dto.response.GroupMemberSummaryResponse;
import com.sep.treksphere.matching.dto.response.GroupSettlementResponse;
import com.sep.treksphere.matching.dto.response.GroupSettlementSummaryResponse;
import com.sep.treksphere.matching.dto.response.MemberBalanceResponse;
import com.sep.treksphere.matching.dto.response.SettlementSuggestionResponse;
import com.sep.treksphere.matching.entity.GroupExpense;
import com.sep.treksphere.matching.entity.GroupExpenseShare;
import com.sep.treksphere.matching.entity.GroupSettlement;
import com.sep.treksphere.matching.entity.GroupTrip;
import com.sep.treksphere.matching.entity.MatchingGroup;
import com.sep.treksphere.matching.entity.MatchingMember;
import com.sep.treksphere.matching.enums.ExpenseShareSettlementStatus;
import com.sep.treksphere.matching.enums.GroupTripStatus;
import com.sep.treksphere.matching.enums.JoinStatus;
import com.sep.treksphere.matching.enums.MatchingRole;
import com.sep.treksphere.matching.enums.SettlementStatus;
import com.sep.treksphere.matching.mapper.GroupSettlementMapper;
import com.sep.treksphere.matching.repository.GroupExpenseRepository;
import com.sep.treksphere.matching.repository.GroupExpenseShareRepository;
import com.sep.treksphere.matching.repository.GroupSettlementRepository;
import com.sep.treksphere.matching.repository.GroupTripRepository;
import com.sep.treksphere.matching.repository.MatchingGroupRepository;
import com.sep.treksphere.matching.repository.MatchingMemberRepository;
import com.sep.treksphere.matching.service.GroupSettlementService;
import com.sep.treksphere.user.User;
import com.sep.treksphere.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GroupSettlementServiceImpl implements GroupSettlementService {

    private static final BigDecimal EPSILON = new BigDecimal("0.01");

    private final MatchingGroupRepository matchingGroupRepository;
    private final MatchingMemberRepository matchingMemberRepository;
    private final UserRepository userRepository;
    private final GroupExpenseRepository groupExpenseRepository;
    private final GroupExpenseShareRepository groupExpenseShareRepository;
    private final GroupSettlementRepository groupSettlementRepository;
    private final GroupTripRepository groupTripRepository;
    private final GroupSettlementMapper groupSettlementMapper;

    @Override
    public GroupSettlementSummaryResponse getSettlementSummary(UUID groupId, String userEmail) {
        MatchingGroup group = getGroupOrThrow(groupId);
        validateMemberAccess(group, userEmail);

        GroupTrip groupTrip = resolveGroupTrip(group);
        List<MatchingMember> activeMembers = matchingMemberRepository
                .findActiveMembers(groupId, JoinStatus.ACCEPTED);

        List<GroupExpense> expenses = groupExpenseRepository.findByGroupTrip_MatchingGroup_MatchingGroupId(groupId);


        BigDecimal totalGroupExpense = expenses.stream()
                .map(GroupExpense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<MemberBalanceResponse> memberBalances = calculateMemberBalances(activeMembers, expenses);
        List<SettlementSuggestionResponse> suggestions = calculateGreedySettlementSuggestions(activeMembers, memberBalances);

        List<GroupSettlement> existingSettlements = groupSettlementRepository
                .findByGroupTrip_MatchingGroup_MatchingGroupIdAndIsDeletedFalseOrderByCreatedAtAsc(groupId);

        boolean isFullySettled = !existingSettlements.isEmpty() && existingSettlements.stream()
                .allMatch(s -> s.getStatus() == SettlementStatus.CONFIRMED);

        return GroupSettlementSummaryResponse.builder()
                .matchingGroupId(groupId)
                .groupTripId(groupTrip.getGroupTripId())
                .totalGroupExpense(totalGroupExpense)
                .memberBalances(memberBalances)
                .suggestions(suggestions)
                .isFullySettled(isFullySettled)
                .build();
    }

    @Override
    public List<GroupSettlementResponse> getSettlements(UUID groupId, String userEmail) {
        MatchingGroup group = getGroupOrThrow(groupId);
        validateMemberAccess(group, userEmail);

        List<GroupSettlement> settlements = groupSettlementRepository
                .findByGroupTrip_MatchingGroup_MatchingGroupIdAndIsDeletedFalseOrderByCreatedAtAsc(groupId);

        return groupSettlementMapper.toResponseList(settlements);
    }

    @Override
    @Transactional
    public List<GroupSettlementResponse> generateSettlements(UUID groupId, String userEmail) {
        MatchingGroup group = getGroupOrThrow(groupId);
        MatchingMember currentMember = validateMemberAccess(group, userEmail);

        if (currentMember.getRole() != MatchingRole.LEADER) {
            throw new AppException(ErrorCode.UNAUTHORIZED_EXPENSE_ACTION);
        }

        GroupTrip groupTrip = resolveGroupTrip(group);
        List<MatchingMember> activeMembers = matchingMemberRepository
                .findActiveMembers(groupId, JoinStatus.ACCEPTED);
        List<GroupExpense> expenses = groupExpenseRepository.findByGroupTrip_MatchingGroup_MatchingGroupId(groupId);


        List<MemberBalanceResponse> balances = calculateMemberBalances(activeMembers, expenses);
        List<SettlementSuggestionResponse> suggestions = calculateGreedySettlementSuggestions(activeMembers, balances);

        if (suggestions.isEmpty()) {
            return Collections.emptyList();
        }

        // Map member by matchingMemberId
        Map<UUID, MatchingMember> memberMap = activeMembers.stream()
                .collect(Collectors.toMap(MatchingMember::getMatchingMemberId, m -> m));

        // Soft delete old unconfirmed settlements (PENDING or REJECTED)
        List<GroupSettlement> existingSettlements = groupSettlementRepository
                .findByGroupTrip_MatchingGroup_MatchingGroupIdAndIsDeletedFalseOrderByCreatedAtAsc(groupId);
        for (GroupSettlement s : existingSettlements) {
            if (s.getStatus() != SettlementStatus.CONFIRMED) {
                s.setIsDeleted(true);
                groupSettlementRepository.save(s);
            }
        }

        List<GroupSettlement> newSettlements = new ArrayList<>();
        for (SettlementSuggestionResponse suggestion : suggestions) {
            MatchingMember from = memberMap.get(suggestion.getFromMember().getMatchingMemberId());
            MatchingMember to = memberMap.get(suggestion.getToMember().getMatchingMemberId());

            if (from == null || to == null) {
                continue;
            }

            GroupSettlement settlement = new GroupSettlement();
            settlement.setGroupTrip(groupTrip);
            settlement.setFromMatchingMember(from);
            settlement.setToMatchingMember(to);
            settlement.setAmount(suggestion.getAmount());
            settlement.setStatus(SettlementStatus.PENDING);
            newSettlements.add(settlement);
        }

        List<GroupSettlement> saved = groupSettlementRepository.saveAll(newSettlements);
        log.info("Leader {} generated {} settlements for group {}", currentMember.getMatchingMemberId(), saved.size(), groupId);

        return groupSettlementMapper.toResponseList(saved);
    }

    @Override
    @Transactional
    public GroupSettlementResponse submitProof(UUID groupId, UUID settlementId, GroupSettlementProofRequest request, String userEmail) {
        MatchingGroup group = getGroupOrThrow(groupId);
        MatchingMember currentMember = validateMemberAccess(group, userEmail);

        GroupSettlement settlement = getSettlementOrThrow(settlementId, groupId);

        if (!settlement.getFromMatchingMember().getMatchingMemberId().equals(currentMember.getMatchingMemberId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED_SETTLEMENT_SUBMIT);
        }

        if (settlement.getStatus() == SettlementStatus.CONFIRMED) {
            throw new AppException(ErrorCode.INVALID_SETTLEMENT_STATUS);
        }

        settlement.setProofUrl(request.getProofUrl().trim());
        settlement.setSubmittedAt(LocalDateTime.now());
        settlement.setStatus(SettlementStatus.PROOF_SUBMITTED);
        settlement.setRejectReason(null);

        GroupSettlement updated = groupSettlementRepository.save(settlement);
        log.info("Debtor {} submitted proof for settlement {}", currentMember.getMatchingMemberId(), settlementId);

        return groupSettlementMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public GroupSettlementResponse confirmSettlement(UUID groupId, UUID settlementId, String userEmail) {
        MatchingGroup group = getGroupOrThrow(groupId);
        MatchingMember currentMember = validateMemberAccess(group, userEmail);

        GroupSettlement settlement = getSettlementOrThrow(settlementId, groupId);

        if (!settlement.getToMatchingMember().getMatchingMemberId().equals(currentMember.getMatchingMemberId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED_SETTLEMENT_CONFIRM);
        }

        if (settlement.getStatus() == SettlementStatus.CONFIRMED) {
            throw new AppException(ErrorCode.INVALID_SETTLEMENT_STATUS);
        }

        settlement.setStatus(SettlementStatus.CONFIRMED);
        settlement.setConfirmedAt(LocalDateTime.now());
        settlement.setConfirmedBy(currentMember);
        settlement.setRejectReason(null);

        GroupSettlement updated = groupSettlementRepository.save(settlement);

        // Update corresponding shares for the settled member if fully settled
        markMemberSharesAsSettledIfEligible(group, settlement.getFromMatchingMember());

        log.info("Payee {} confirmed settlement {}", currentMember.getMatchingMemberId(), settlementId);
        return groupSettlementMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public GroupSettlementResponse rejectSettlement(UUID groupId, UUID settlementId, GroupSettlementRejectRequest request, String userEmail) {
        MatchingGroup group = getGroupOrThrow(groupId);
        MatchingMember currentMember = validateMemberAccess(group, userEmail);

        GroupSettlement settlement = getSettlementOrThrow(settlementId, groupId);

        if (!settlement.getToMatchingMember().getMatchingMemberId().equals(currentMember.getMatchingMemberId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED_SETTLEMENT_REJECT);
        }

        if (settlement.getStatus() == SettlementStatus.CONFIRMED) {
            throw new AppException(ErrorCode.INVALID_SETTLEMENT_STATUS);
        }

        settlement.setStatus(SettlementStatus.REJECTED);
        settlement.setRejectReason(request.getReason().trim());

        GroupSettlement updated = groupSettlementRepository.save(settlement);
        log.info("Payee {} rejected settlement {} with reason: {}", currentMember.getMatchingMemberId(), settlementId, request.getReason());

        return groupSettlementMapper.toResponse(updated);
    }

    // ==================== Private Helper Methods ====================

    private MatchingGroup getGroupOrThrow(UUID groupId) {
        return matchingGroupRepository.findById(groupId)
                .filter(g -> !Boolean.TRUE.equals(g.getIsDeleted()))
                .orElseThrow(() -> new AppException(ErrorCode.MATCHING_GROUP_NOT_FOUND));
    }

    private MatchingMember validateMemberAccess(MatchingGroup group, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        return matchingMemberRepository.findByMatchingGroupAndUserAndIsDeletedFalse(group, user)
                .filter(m -> m.getStatus() == JoinStatus.ACCEPTED)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_ACCEPTED_MATCHING_MEMBER));
    }

    private GroupTrip resolveGroupTrip(MatchingGroup group) {
        return groupTripRepository.findByMatchingGroup(group)
                .orElseGet(() -> {
                    GroupTrip trip = new GroupTrip();
                    trip.setMatchingGroup(group);
                    trip.setStatus(GroupTripStatus.PLANNED);
                    trip.setScheduledStartAt(group.getTargetDate() != null
                            ? group.getTargetDate().atStartOfDay()
                            : LocalDateTime.now());
                    return groupTripRepository.save(trip);
                });
    }

    private GroupSettlement getSettlementOrThrow(UUID settlementId, UUID groupId) {
        return groupSettlementRepository
                .findByGroupSettlementIdAndGroupTrip_MatchingGroup_MatchingGroupIdAndIsDeletedFalse(settlementId, groupId)
                .orElseThrow(() -> new AppException(ErrorCode.GROUP_SETTLEMENT_NOT_FOUND));
    }


    private List<MemberBalanceResponse> calculateMemberBalances(List<MatchingMember> activeMembers, List<GroupExpense> expenses) {
        List<MemberBalanceResponse> responses = new ArrayList<>();

        for (MatchingMember member : activeMembers) {
            UUID memberId = member.getMatchingMemberId();

            BigDecimal totalPaid = expenses.stream()
                    .filter(e -> e.getPaidBy() != null && memberId.equals(e.getPaidBy().getMatchingMemberId()))
                    .map(GroupExpense::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(2, RoundingMode.HALF_UP);

            BigDecimal totalShare = expenses.stream()
                    .flatMap(e -> e.getShares().stream())
                    .filter(s -> !Boolean.TRUE.equals(s.getIsDeleted()))
                    .filter(s -> s.getMatchingMember() != null && memberId.equals(s.getMatchingMember().getMatchingMemberId()))
                    .map(GroupExpenseShare::getShareAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(2, RoundingMode.HALF_UP);

            BigDecimal netBalance = totalPaid.subtract(totalShare).setScale(2, RoundingMode.HALF_UP);

            String balanceType;
            if (netBalance.compareTo(EPSILON) > 0) {
                balanceType = "CREDITOR";
            } else if (netBalance.compareTo(EPSILON.negate()) < 0) {
                balanceType = "DEBTOR";
            } else {
                balanceType = "BALANCED";
            }

            responses.add(MemberBalanceResponse.builder()
                    .member(groupSettlementMapper.toMemberSummary(member))
                    .totalPaid(totalPaid)
                    .totalShare(totalShare)
                    .netBalance(netBalance)
                    .balanceType(balanceType)
                    .build());
        }

        return responses;
    }

    private List<SettlementSuggestionResponse> calculateGreedySettlementSuggestions(
            List<MatchingMember> activeMembers,
            List<MemberBalanceResponse> balances) {

        Map<UUID, MatchingMember> memberMap = activeMembers.stream()
                .collect(Collectors.toMap(MatchingMember::getMatchingMemberId, m -> m));

        // Mutable balance items for greedy matching
        List<MutableBalanceItem> creditors = new ArrayList<>();
        List<MutableBalanceItem> debtors = new ArrayList<>();

        for (MemberBalanceResponse b : balances) {
            MatchingMember member = memberMap.get(b.getMember().getMatchingMemberId());
            if (member == null) continue;

            if (b.getNetBalance().compareTo(EPSILON) > 0) {
                creditors.add(new MutableBalanceItem(member, b.getNetBalance()));
            } else if (b.getNetBalance().compareTo(EPSILON.negate()) < 0) {
                debtors.add(new MutableBalanceItem(member, b.getNetBalance().abs()));
            }
        }

        // Sort descending by remaining amount
        creditors.sort(Comparator.comparing(MutableBalanceItem::getRemainingAmount).reversed());
        debtors.sort(Comparator.comparing(MutableBalanceItem::getRemainingAmount).reversed());

        List<SettlementSuggestionResponse> suggestions = new ArrayList<>();
        int cIdx = 0;
        int dIdx = 0;

        while (cIdx < creditors.size() && dIdx < debtors.size()) {
            MutableBalanceItem creditor = creditors.get(cIdx);
            MutableBalanceItem debtor = debtors.get(dIdx);

            BigDecimal settleAmount = debtor.remainingAmount.min(creditor.remainingAmount)
                    .setScale(2, RoundingMode.HALF_UP);

            if (settleAmount.compareTo(BigDecimal.ZERO) > 0) {
                suggestions.add(SettlementSuggestionResponse.builder()
                        .fromMember(groupSettlementMapper.toMemberSummary(debtor.member))
                        .toMember(groupSettlementMapper.toMemberSummary(creditor.member))
                        .amount(settleAmount)
                        .build());
            }

            debtor.remainingAmount = debtor.remainingAmount.subtract(settleAmount);
            creditor.remainingAmount = creditor.remainingAmount.subtract(settleAmount);

            if (debtor.remainingAmount.compareTo(EPSILON) < 0) {
                dIdx++;
            }
            if (creditor.remainingAmount.compareTo(EPSILON) < 0) {
                cIdx++;
            }
        }

        return suggestions;
    }

    private void markMemberSharesAsSettledIfEligible(MatchingGroup group, MatchingMember member) {
        List<GroupSettlement> memberDebts = groupSettlementRepository
                .findByGroupTrip_MatchingGroup_MatchingGroupIdAndIsDeletedFalseOrderByCreatedAtAsc(group.getMatchingGroupId())
                .stream()
                .filter(s -> s.getFromMatchingMember().getMatchingMemberId().equals(member.getMatchingMemberId()))
                .toList();

        boolean allDebtsSettled = memberDebts.stream().allMatch(s -> s.getStatus() == SettlementStatus.CONFIRMED);
        if (allDebtsSettled) {
            List<GroupExpenseShare> memberShares = groupExpenseShareRepository
                    .findByMatchingMember_MatchingMemberId(member.getMatchingMemberId())
                    .stream()
                    .filter(s -> !Boolean.TRUE.equals(s.getIsDeleted()))
                    .toList();
            for (GroupExpenseShare share : memberShares) {
                share.setSettlementStatus(ExpenseShareSettlementStatus.SETTLED);
                share.setSettledAt(LocalDateTime.now());
                groupExpenseShareRepository.save(share);
            }
        }
    }

    private static class MutableBalanceItem {
        private final MatchingMember member;
        private BigDecimal remainingAmount;

        public MutableBalanceItem(MatchingMember member, BigDecimal remainingAmount) {
            this.member = member;
            this.remainingAmount = remainingAmount;
        }

        public BigDecimal getRemainingAmount() {
            return remainingAmount;
        }
    }
}
