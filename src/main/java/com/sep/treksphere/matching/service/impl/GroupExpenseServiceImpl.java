package com.sep.treksphere.matching.service.impl;

import com.sep.treksphere.common.dto.PaginationResponse;
import com.sep.treksphere.common.exception.AppException;
import com.sep.treksphere.common.exception.ErrorCode;
import com.sep.treksphere.common.util.PaginationUtils;
import com.sep.treksphere.matching.dto.request.GroupExpenseCreateRequest;
import com.sep.treksphere.matching.dto.request.GroupExpenseCustomShareRequest;
import com.sep.treksphere.matching.dto.request.GroupExpenseFilterRequest;
import com.sep.treksphere.matching.dto.request.GroupExpenseUpdateRequest;
import com.sep.treksphere.matching.dto.response.GroupExpenseResponse;
import com.sep.treksphere.matching.dto.response.GroupExpenseSummaryResponse;
import com.sep.treksphere.matching.entity.GroupExpense;
import com.sep.treksphere.matching.entity.GroupExpenseShare;
import com.sep.treksphere.matching.entity.GroupTrip;
import com.sep.treksphere.matching.entity.MatchingGroup;
import com.sep.treksphere.matching.entity.MatchingMember;
import com.sep.treksphere.matching.enums.BeneficiaryScope;
import com.sep.treksphere.matching.enums.ExpenseShareSettlementStatus;
import com.sep.treksphere.matching.enums.GroupTripStatus;
import com.sep.treksphere.matching.enums.JoinStatus;
import com.sep.treksphere.matching.enums.MatchingRole;
import com.sep.treksphere.matching.enums.SplitMethod;
import com.sep.treksphere.matching.mapper.GroupExpenseMapper;
import com.sep.treksphere.matching.repository.GroupExpenseRepository;
import com.sep.treksphere.matching.repository.GroupExpenseShareRepository;
import com.sep.treksphere.matching.repository.GroupTripRepository;
import com.sep.treksphere.matching.repository.MatchingGroupRepository;
import com.sep.treksphere.matching.repository.MatchingMemberRepository;
import com.sep.treksphere.matching.service.GroupExpenseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class GroupExpenseServiceImpl implements GroupExpenseService {

    private final GroupExpenseRepository groupExpenseRepository;
    private final GroupExpenseShareRepository groupExpenseShareRepository;
    private final MatchingGroupRepository matchingGroupRepository;
    private final MatchingMemberRepository matchingMemberRepository;
    private final GroupTripRepository groupTripRepository;
    private final GroupExpenseMapper groupExpenseMapper;

    @Override
    @Transactional
    public GroupExpenseResponse createExpense(UUID groupId, GroupExpenseCreateRequest request, UUID currentUserId) {
        log.info("Creating group expense for group: {}, userId: {}", groupId, currentUserId);

        MatchingGroup group = getMatchingGroup(groupId);
        MatchingMember callerMember = getActiveMember(groupId, currentUserId);
        validateLeaderPermission(callerMember);

        GroupTrip groupTrip = resolveGroupTrip(group);

        MatchingMember paidBy = resolvePayer(group, callerMember, request.getPaidByMemberId());
        List<MatchingMember> beneficiaries = resolveBeneficiaries(group, request.getBeneficiaryScope(), request.getBeneficiaryMemberIds());

        GroupExpense expense = groupExpenseMapper.toEntity(request);
        expense.setGroupTrip(groupTrip);
        expense.setPaidBy(paidBy);
        expense.setBeneficiaryCount(beneficiaries.size());
        if (expense.getSpentAt() == null) {
            expense.setSpentAt(LocalDateTime.now());
        }
        if (expense.getSplitMethod() == null) {
            expense.setSplitMethod(SplitMethod.EQUAL);
        }

        expense = groupExpenseRepository.save(expense);

        List<GroupExpenseShare> shares;
        if (expense.getSplitMethod() == SplitMethod.CUSTOM) {
            shares = generateCustomShares(expense, beneficiaries, request.getCustomShares(), expense.getAmount());
        } else {
            shares = generateEqualShares(expense, beneficiaries, expense.getAmount());
        }
        expense.setShares(shares);

        return groupExpenseMapper.toResponse(expense);
    }

    @Override
    @Transactional
    public GroupExpenseResponse updateExpense(UUID groupId, UUID expenseId, GroupExpenseUpdateRequest request, UUID currentUserId) {
        log.info("Updating group expense: {} for group: {}, userId: {}", expenseId, groupId, currentUserId);

        MatchingGroup group = getMatchingGroup(groupId);
        MatchingMember callerMember = getActiveMember(groupId, currentUserId);
        validateLeaderPermission(callerMember);

        GroupExpense expense = getGroupExpense(expenseId, groupId);

        groupExpenseMapper.updateEntityFromRequest(request, expense);

        if (request.getPaidByMemberId() != null) {
            MatchingMember paidBy = resolvePayer(group, callerMember, request.getPaidByMemberId());
            expense.setPaidBy(paidBy);
        }

        boolean needRecomputeShares = false;
        BeneficiaryScope scope = expense.getBeneficiaryScope();
        List<UUID> beneficiaryIds = request.getBeneficiaryMemberIds();

        if (request.getBeneficiaryScope() != null || request.getBeneficiaryMemberIds() != null
                || request.getAmount() != null || request.getSplitMethod() != null
                || request.getCustomShares() != null) {
            needRecomputeShares = true;
        }

        if (needRecomputeShares) {
            List<MatchingMember> beneficiaries = resolveBeneficiaries(group, scope, beneficiaryIds);
            expense.setBeneficiaryCount(beneficiaries.size());

            if (expense.getSplitMethod() == SplitMethod.CUSTOM) {
                syncCustomExpenseShares(expense, beneficiaries, request.getCustomShares(), expense.getAmount());
            } else {
                syncEqualExpenseShares(expense, beneficiaries, expense.getAmount());
            }
        }

        GroupExpense updated = groupExpenseRepository.save(expense);
        return groupExpenseMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public void voidExpense(UUID groupId, UUID expenseId, UUID currentUserId) {
        log.info("Voiding group expense: {} for group: {}, userId: {}", expenseId, groupId, currentUserId);

        getMatchingGroup(groupId);
        MatchingMember callerMember = getActiveMember(groupId, currentUserId);
        validateLeaderPermission(callerMember);

        GroupExpense expense = getGroupExpense(expenseId, groupId);

        LocalDateTime now = LocalDateTime.now();
        expense.setIsDeleted(true);
        expense.setDeletedAt(now);

        if (expense.getShares() != null) {
            for (GroupExpenseShare share : expense.getShares()) {
                share.setIsDeleted(true);
                share.setDeletedAt(now);
            }
        }

        groupExpenseRepository.save(expense);
    }

    @Override
    public GroupExpenseResponse getExpenseDetail(UUID groupId, UUID expenseId, UUID currentUserId) {
        getMatchingGroup(groupId);
        getActiveMember(groupId, currentUserId);

        GroupExpense expense = getGroupExpense(expenseId, groupId);
        return groupExpenseMapper.toResponse(expense);
    }

    @Override
    public PaginationResponse<GroupExpenseResponse> getGroupExpenses(UUID groupId, GroupExpenseFilterRequest filter, UUID currentUserId) {
        getMatchingGroup(groupId);
        getActiveMember(groupId, currentUserId);

        GroupExpenseFilterRequest safeFilter = filter != null ? filter : new GroupExpenseFilterRequest();
        Pageable pageable = safeFilter.getPageable();

        String keyword = safeFilter.getKeyword();
        if (keyword != null && keyword.trim().isEmpty()) {
            keyword = null;
        }

        Page<GroupExpense> page = groupExpenseRepository.findByGroupIdWithFilter(
                groupId,
                keyword,
                safeFilter.getPaidByMemberId(),
                safeFilter.getBeneficiaryScope(),
                pageable
        );
        return PaginationUtils.toPaginationResponse(page.map(groupExpenseMapper::toResponse));
    }

    @Override
    public GroupExpenseSummaryResponse getExpenseSummary(UUID groupId, UUID currentUserId) {
        MatchingGroup group = getMatchingGroup(groupId);
        getActiveMember(groupId, currentUserId);

        GroupTrip groupTrip = groupTripRepository.findByMatchingGroup(group).orElse(null);
        UUID groupTripId = groupTrip != null ? groupTrip.getGroupTripId() : null;

        BigDecimal totalExpense = groupExpenseRepository.sumAmountByMatchingGroupId(groupId);
        if (totalExpense == null) {
            totalExpense = BigDecimal.ZERO;
        }

        long totalCount = groupExpenseRepository.countByMatchingGroupId(groupId);
        long activeMemberCount = matchingMemberRepository.countActiveMembersByGroupIdAndStatus(groupId, JoinStatus.ACCEPTED);

        BigDecimal avgPerMember = BigDecimal.ZERO;
        if (activeMemberCount > 0 && totalExpense.compareTo(BigDecimal.ZERO) > 0) {
            avgPerMember = totalExpense.divide(BigDecimal.valueOf(activeMemberCount), 2, RoundingMode.HALF_UP);
        }

        return GroupExpenseSummaryResponse.builder()
                .matchingGroupId(groupId)
                .groupTripId(groupTripId)
                .totalExpenseAmount(totalExpense)
                .totalExpensesCount((int) totalCount)
                .activeMemberCount((int) activeMemberCount)
                .averageExpensePerMember(avgPerMember)
                .build();
    }

    private MatchingGroup getMatchingGroup(UUID groupId) {
        return matchingGroupRepository.findById(groupId)
                .filter(g -> !Boolean.TRUE.equals(g.getIsDeleted()))
                .orElseThrow(() -> new AppException(ErrorCode.MATCHING_GROUP_NOT_FOUND));
    }

    private MatchingMember getActiveMember(UUID groupId, UUID currentUserId) {
        if (currentUserId == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        return matchingMemberRepository.findByGroupIdAndUserId(groupId, currentUserId)
                .filter(m -> m.getStatus() == JoinStatus.ACCEPTED)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_ACCEPTED_MATCHING_MEMBER));
    }

    private GroupExpense getGroupExpense(UUID expenseId, UUID groupId) {
        return groupExpenseRepository.findByGroupExpenseIdAndGroupTrip_MatchingGroup_MatchingGroupId(expenseId, groupId)
                .orElseThrow(() -> new AppException(ErrorCode.GROUP_EXPENSE_NOT_FOUND));
    }

    private GroupTrip resolveGroupTrip(MatchingGroup group) {
        return groupTripRepository.findByMatchingGroup(group)
                .orElseGet(() -> {
                    GroupTrip trip = new GroupTrip();
                    trip.setMatchingGroup(group);
                    trip.setStatus(GroupTripStatus.PLANNED);
                    LocalDateTime scheduled = group.getTargetDate() != null
                            ? group.getTargetDate().atStartOfDay()
                            : LocalDateTime.now();
                    trip.setScheduledStartAt(scheduled);
                    return groupTripRepository.save(trip);
                });
    }

    private MatchingMember resolvePayer(MatchingGroup group, MatchingMember callerMember, UUID paidByMemberId) {
        if (paidByMemberId == null) {
            return callerMember;
        }
        return matchingMemberRepository.findMemberByIdAndGroupId(paidByMemberId, group.getMatchingGroupId())
                .filter(m -> m.getStatus() == JoinStatus.ACCEPTED)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_EXPENSE_BENEFICIARIES));
    }

    private List<MatchingMember> resolveBeneficiaries(MatchingGroup group, BeneficiaryScope scope, List<UUID> selectedMemberIds) {
        List<MatchingMember> activeMembers = matchingMemberRepository.findActiveMembers(group.getMatchingGroupId(), JoinStatus.ACCEPTED);

        if (scope == BeneficiaryScope.ALL_MEMBERS || scope == null) {
            if (activeMembers.isEmpty()) {
                throw new AppException(ErrorCode.INVALID_EXPENSE_BENEFICIARIES);
            }
            return activeMembers;
        }

        if (selectedMemberIds == null || selectedMemberIds.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_EXPENSE_BENEFICIARIES);
        }

        Set<UUID> selectedIdSet = Set.copyOf(selectedMemberIds);
        List<MatchingMember> matched = activeMembers.stream()
                .filter(m -> selectedIdSet.contains(m.getMatchingMemberId()))
                .toList();

        if (matched.size() != selectedIdSet.size()) {
            throw new AppException(ErrorCode.INVALID_EXPENSE_BENEFICIARIES);
        }

        return matched;
    }

    private void validateLeaderPermission(MatchingMember callerMember) {
        if (callerMember.getRole() != MatchingRole.LEADER) {
            throw new AppException(ErrorCode.UNAUTHORIZED_EXPENSE_ACTION);
        }
    }

    private List<GroupExpenseShare> generateEqualShares(GroupExpense expense, List<MatchingMember> beneficiaries, BigDecimal totalAmount) {
        int count = beneficiaries.size();
        if (count == 0) {
            return new ArrayList<>();
        }

        BigDecimal baseShare = totalAmount.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
        BigDecimal currentSum = BigDecimal.ZERO;
        List<GroupExpenseShare> shares = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            MatchingMember member = beneficiaries.get(i);
            BigDecimal shareAmount;
            if (i == count - 1) {
                // Ensure exact sum match
                shareAmount = totalAmount.subtract(currentSum);
            } else {
                shareAmount = baseShare;
                currentSum = currentSum.add(shareAmount);
            }

            GroupExpenseShare share = new GroupExpenseShare();
            share.setGroupExpense(expense);
            share.setMatchingMember(member);
            share.setShareAmount(shareAmount);
            share.setSettlementStatus(ExpenseShareSettlementStatus.UNSETTLED);
            shares.add(share);
        }

        return groupExpenseShareRepository.saveAll(shares);
    }

    private List<GroupExpenseShare> generateCustomShares(GroupExpense expense, List<MatchingMember> beneficiaries,
                                                         List<GroupExpenseCustomShareRequest> customShares, BigDecimal totalAmount) {
        Map<UUID, BigDecimal> customShareMap = validateAndMapCustomShares(beneficiaries, customShares, totalAmount);
        List<GroupExpenseShare> shares = new ArrayList<>();

        for (MatchingMember member : beneficiaries) {
            BigDecimal shareAmount = customShareMap.get(member.getMatchingMemberId());
            GroupExpenseShare share = new GroupExpenseShare();
            share.setGroupExpense(expense);
            share.setMatchingMember(member);
            share.setShareAmount(shareAmount);
            share.setSettlementStatus(ExpenseShareSettlementStatus.UNSETTLED);
            shares.add(share);
        }

        return groupExpenseShareRepository.saveAll(shares);
    }

    private void syncEqualExpenseShares(GroupExpense expense, List<MatchingMember> beneficiaries, BigDecimal totalAmount) {
        int count = beneficiaries.size();
        if (count == 0) {
            return;
        }

        BigDecimal baseShare = totalAmount.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
        BigDecimal currentSum = BigDecimal.ZERO;

        List<GroupExpenseShare> currentShares = expense.getShares() != null ? expense.getShares() : new ArrayList<>();
        Map<UUID, GroupExpenseShare> shareByMemberId = currentShares.stream()
                .filter(s -> !Boolean.TRUE.equals(s.getIsDeleted()))
                .collect(Collectors.toMap(s -> s.getMatchingMember().getMatchingMemberId(), s -> s, (a, b) -> a));

        Set<UUID> targetMemberIds = beneficiaries.stream()
                .map(MatchingMember::getMatchingMemberId)
                .collect(Collectors.toSet());

        LocalDateTime now = LocalDateTime.now();
        for (GroupExpenseShare share : currentShares) {
            if (!targetMemberIds.contains(share.getMatchingMember().getMatchingMemberId())) {
                share.setIsDeleted(true);
                share.setDeletedAt(now);
            }
        }

        for (int i = 0; i < count; i++) {
            MatchingMember member = beneficiaries.get(i);
            BigDecimal shareAmount;
            if (i == count - 1) {
                shareAmount = totalAmount.subtract(currentSum);
            } else {
                shareAmount = baseShare;
                currentSum = currentSum.add(shareAmount);
            }

            GroupExpenseShare existing = shareByMemberId.get(member.getMatchingMemberId());
            if (existing != null) {
                existing.setShareAmount(shareAmount);
                existing.setIsDeleted(false);
                existing.setDeletedAt(null);
            } else {
                GroupExpenseShare newShare = new GroupExpenseShare();
                newShare.setGroupExpense(expense);
                newShare.setMatchingMember(member);
                newShare.setShareAmount(shareAmount);
                newShare.setSettlementStatus(ExpenseShareSettlementStatus.UNSETTLED);
                newShare.setIsDeleted(false);
                currentShares.add(newShare);
            }
        }
        expense.setShares(currentShares);
    }

    private void syncCustomExpenseShares(GroupExpense expense, List<MatchingMember> beneficiaries,
                                         List<GroupExpenseCustomShareRequest> customShares, BigDecimal totalAmount) {
        Map<UUID, BigDecimal> customShareMap = validateAndMapCustomShares(beneficiaries, customShares, totalAmount);

        List<GroupExpenseShare> currentShares = expense.getShares() != null ? expense.getShares() : new ArrayList<>();
        Map<UUID, GroupExpenseShare> shareByMemberId = currentShares.stream()
                .filter(s -> !Boolean.TRUE.equals(s.getIsDeleted()))
                .collect(Collectors.toMap(s -> s.getMatchingMember().getMatchingMemberId(), s -> s, (a, b) -> a));

        Set<UUID> targetMemberIds = beneficiaries.stream()
                .map(MatchingMember::getMatchingMemberId)
                .collect(Collectors.toSet());

        LocalDateTime now = LocalDateTime.now();
        for (GroupExpenseShare share : currentShares) {
            if (!targetMemberIds.contains(share.getMatchingMember().getMatchingMemberId())) {
                share.setIsDeleted(true);
                share.setDeletedAt(now);
            }
        }

        for (MatchingMember member : beneficiaries) {
            BigDecimal shareAmount = customShareMap.get(member.getMatchingMemberId());
            GroupExpenseShare existing = shareByMemberId.get(member.getMatchingMemberId());
            if (existing != null) {
                existing.setShareAmount(shareAmount);
                existing.setIsDeleted(false);
                existing.setDeletedAt(null);
            } else {
                GroupExpenseShare newShare = new GroupExpenseShare();
                newShare.setGroupExpense(expense);
                newShare.setMatchingMember(member);
                newShare.setShareAmount(shareAmount);
                newShare.setSettlementStatus(ExpenseShareSettlementStatus.UNSETTLED);
                newShare.setIsDeleted(false);
                currentShares.add(newShare);
            }
        }
        expense.setShares(currentShares);
    }

    private Map<UUID, BigDecimal> validateAndMapCustomShares(List<MatchingMember> beneficiaries,
                                                             List<GroupExpenseCustomShareRequest> customShares,
                                                             BigDecimal totalAmount) {
        if (customShares == null || customShares.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_EXPENSE_CUSTOM_SPLIT_MEMBERS);
        }

        Set<UUID> beneficiaryIdSet = beneficiaries.stream()
                .map(MatchingMember::getMatchingMemberId)
                .collect(Collectors.toSet());

        Set<UUID> providedMemberIds = new HashSet<>();
        Map<UUID, BigDecimal> customShareMap = new HashMap<>();
        BigDecimal sum = BigDecimal.ZERO;

        for (GroupExpenseCustomShareRequest item : customShares) {
            if (item == null || item.getMatchingMemberId() == null || item.getAmount() == null) {
                throw new AppException(ErrorCode.INVALID_EXPENSE_CUSTOM_SPLIT_SUM);
            }
            if (item.getAmount().compareTo(BigDecimal.ZERO) < 0) {
                throw new AppException(ErrorCode.INVALID_EXPENSE_CUSTOM_SPLIT_SUM);
            }
            if (!beneficiaryIdSet.contains(item.getMatchingMemberId())) {
                throw new AppException(ErrorCode.INVALID_EXPENSE_CUSTOM_SPLIT_MEMBERS);
            }
            if (!providedMemberIds.add(item.getMatchingMemberId())) {
                throw new AppException(ErrorCode.INVALID_EXPENSE_BENEFICIARIES);
            }
            customShareMap.put(item.getMatchingMemberId(), item.getAmount());
            sum = sum.add(item.getAmount());
        }

        if (providedMemberIds.size() != beneficiaries.size()) {
            throw new AppException(ErrorCode.INVALID_EXPENSE_CUSTOM_SPLIT_MEMBERS);
        }

        if (sum.compareTo(totalAmount) != 0) {
            throw new AppException(ErrorCode.INVALID_EXPENSE_CUSTOM_SPLIT_SUM);
        }

        return customShareMap;
    }
}
