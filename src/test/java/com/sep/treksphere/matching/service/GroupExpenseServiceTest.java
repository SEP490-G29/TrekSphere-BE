package com.sep.treksphere.matching.service;

import com.sep.treksphere.common.dto.PaginationResponse;
import com.sep.treksphere.common.exception.AppException;
import com.sep.treksphere.common.exception.ErrorCode;
import com.sep.treksphere.matching.dto.request.GroupExpenseCreateRequest;
import com.sep.treksphere.matching.dto.request.GroupExpenseFilterRequest;
import com.sep.treksphere.matching.dto.request.GroupExpenseUpdateRequest;
import com.sep.treksphere.matching.dto.response.GroupExpenseResponse;
import com.sep.treksphere.matching.dto.response.GroupExpenseSummaryResponse;
import com.sep.treksphere.matching.entity.GroupExpense;
import com.sep.treksphere.matching.entity.GroupTrip;
import com.sep.treksphere.matching.entity.MatchingGroup;
import com.sep.treksphere.matching.entity.MatchingMember;
import com.sep.treksphere.matching.enums.BeneficiaryScope;
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
import com.sep.treksphere.matching.service.impl.GroupExpenseServiceImpl;
import com.sep.treksphere.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GroupExpenseServiceTest {

    @Mock
    private GroupExpenseRepository groupExpenseRepository;

    @Mock
    private GroupExpenseShareRepository groupExpenseShareRepository;

    @Mock
    private MatchingGroupRepository matchingGroupRepository;

    @Mock
    private MatchingMemberRepository matchingMemberRepository;

    @Mock
    private GroupTripRepository groupTripRepository;

    @Spy
    private GroupExpenseMapper groupExpenseMapper = Mappers.getMapper(GroupExpenseMapper.class);

    @InjectMocks
    private GroupExpenseServiceImpl groupExpenseService;

    private UUID groupId;
    private UUID leaderUserId;
    private UUID memberUserId;
    private UUID outsiderUserId;

    private MatchingGroup group;
    private GroupTrip trip;
    private User leaderUser;
    private User memberUser;
    private MatchingMember leaderMember;
    private MatchingMember regularMember;

    @BeforeEach
    void setUp() {
        groupId = UUID.randomUUID();
        leaderUserId = UUID.randomUUID();
        memberUserId = UUID.randomUUID();
        outsiderUserId = UUID.randomUUID();

        leaderUser = new User();
        leaderUser.setUserId(leaderUserId);
        leaderUser.setEmail("leader@example.com");
        leaderUser.setFullName("Leader User");

        memberUser = new User();
        memberUser.setUserId(memberUserId);
        memberUser.setEmail("member@example.com");
        memberUser.setFullName("Member User");

        group = new MatchingGroup();
        group.setMatchingGroupId(groupId);
        group.setTargetDate(LocalDate.now().plusDays(5));
        group.setIsDeleted(false);

        trip = new GroupTrip();
        trip.setGroupTripId(UUID.randomUUID());
        trip.setMatchingGroup(group);
        trip.setStatus(GroupTripStatus.PLANNED);
        trip.setScheduledStartAt(group.getTargetDate().atStartOfDay());

        leaderMember = new MatchingMember();
        leaderMember.setMatchingMemberId(UUID.randomUUID());
        leaderMember.setMatchingGroup(group);
        leaderMember.setUser(leaderUser);
        leaderMember.setRole(MatchingRole.LEADER);
        leaderMember.setStatus(JoinStatus.ACCEPTED);

        regularMember = new MatchingMember();
        regularMember.setMatchingMemberId(UUID.randomUUID());
        regularMember.setMatchingGroup(group);
        regularMember.setUser(memberUser);
        regularMember.setRole(MatchingRole.MEMBER);
        regularMember.setStatus(JoinStatus.ACCEPTED);
    }

    @Test
    void createExpense_Success_AllMembers() {
        GroupExpenseCreateRequest request = GroupExpenseCreateRequest.builder()
                .title("Bữa tối lẩu cá hồi")
                .amount(new BigDecimal("600000.00"))
                .beneficiaryScope(BeneficiaryScope.ALL_MEMBERS)
                .splitMethod(SplitMethod.EQUAL)
                .build();

        when(matchingGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(matchingMemberRepository.findByGroupIdAndUserId(groupId, leaderUserId))
                .thenReturn(Optional.of(leaderMember));
        when(groupTripRepository.findByMatchingGroup(group)).thenReturn(Optional.of(trip));
        when(matchingMemberRepository.findActiveMembers(groupId, JoinStatus.ACCEPTED))
                .thenReturn(List.of(leaderMember, regularMember));
        when(groupExpenseRepository.save(any(GroupExpense.class))).thenAnswer(invocation -> {
            GroupExpense e = invocation.getArgument(0);
            e.setGroupExpenseId(UUID.randomUUID());
            return e;
        });
        when(groupExpenseShareRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        GroupExpenseResponse response = groupExpenseService.createExpense(groupId, request, leaderUserId);

        assertThat(response).isNotNull();
        assertThat(response.getTitle()).isEqualTo("Bữa tối lẩu cá hồi");
        assertThat(response.getAmount()).isEqualByComparingTo("600000.00");
        assertThat(response.getBeneficiaryCount()).isEqualTo(2);
        assertThat(response.getPayer().getMatchingMemberId()).isEqualTo(leaderMember.getMatchingMemberId());
        assertThat(response.getPayer().getUserId()).isEqualTo(leaderUserId);
        assertThat(response.getShares()).hasSize(2);
        assertThat(response.getShares().get(0).getShareAmount()).isEqualByComparingTo("300000.00");
        assertThat(response.getShares().get(1).getShareAmount()).isEqualByComparingTo("300000.00");

        verify(groupExpenseRepository).save(any(GroupExpense.class));
        verify(groupExpenseShareRepository).saveAll(any());
    }

    @Test
    void createExpense_Success_SelectedMembers() {
        GroupExpenseCreateRequest request = GroupExpenseCreateRequest.builder()
                .title("Vé cáp treo riêng")
                .amount(new BigDecimal("250000.00"))
                .beneficiaryScope(BeneficiaryScope.SELECTED_MEMBERS)
                .beneficiaryMemberIds(List.of(regularMember.getMatchingMemberId()))
                .splitMethod(SplitMethod.EQUAL)
                .build();

        when(matchingGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(matchingMemberRepository.findByGroupIdAndUserId(groupId, leaderUserId))
                .thenReturn(Optional.of(leaderMember));
        when(groupTripRepository.findByMatchingGroup(group)).thenReturn(Optional.of(trip));
        when(matchingMemberRepository.findActiveMembers(groupId, JoinStatus.ACCEPTED))
                .thenReturn(List.of(leaderMember, regularMember));
        when(groupExpenseRepository.save(any(GroupExpense.class))).thenAnswer(invocation -> {
            GroupExpense e = invocation.getArgument(0);
            e.setGroupExpenseId(UUID.randomUUID());
            return e;
        });
        when(groupExpenseShareRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        GroupExpenseResponse response = groupExpenseService.createExpense(groupId, request, leaderUserId);

        assertThat(response).isNotNull();
        assertThat(response.getBeneficiaryCount()).isEqualTo(1);
        assertThat(response.getShares()).hasSize(1);
        assertThat(response.getShares().get(0).getMember().getMatchingMemberId()).isEqualTo(regularMember.getMatchingMemberId());
    }

    @Test
    void createExpense_ThrowsWhenNotLeader() {
        GroupExpenseCreateRequest request = GroupExpenseCreateRequest.builder()
                .title("Chi tiêu trái phép")
                .amount(new BigDecimal("100000.00"))
                .beneficiaryScope(BeneficiaryScope.ALL_MEMBERS)
                .build();

        when(matchingGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(matchingMemberRepository.findByGroupIdAndUserId(groupId, memberUserId))
                .thenReturn(Optional.of(regularMember));

        assertThatThrownBy(() -> groupExpenseService.createExpense(groupId, request, memberUserId))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.UNAUTHORIZED_EXPENSE_ACTION);
    }

    @Test
    void createExpense_ThrowsWhenNotAcceptedMember() {
        GroupExpenseCreateRequest request = GroupExpenseCreateRequest.builder()
                .title("Chi tiêu trái phép")
                .amount(new BigDecimal("100000.00"))
                .beneficiaryScope(BeneficiaryScope.ALL_MEMBERS)
                .build();

        when(matchingGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(matchingMemberRepository.findByGroupIdAndUserId(groupId, outsiderUserId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> groupExpenseService.createExpense(groupId, request, outsiderUserId))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_ACCEPTED_MATCHING_MEMBER);
    }

    @Test
    void createExpense_ThrowsWhenInvalidBeneficiaries() {
        GroupExpenseCreateRequest request = GroupExpenseCreateRequest.builder()
                .title("Chi tiêu lỗi người nhận")
                .amount(new BigDecimal("100000.00"))
                .beneficiaryScope(BeneficiaryScope.SELECTED_MEMBERS)
                .beneficiaryMemberIds(List.of(UUID.randomUUID())) // Non-existent member ID
                .build();

        when(matchingGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(matchingMemberRepository.findByGroupIdAndUserId(groupId, leaderUserId))
                .thenReturn(Optional.of(leaderMember));
        when(groupTripRepository.findByMatchingGroup(group)).thenReturn(Optional.of(trip));
        when(matchingMemberRepository.findActiveMembers(groupId, JoinStatus.ACCEPTED))
                .thenReturn(List.of(leaderMember, regularMember));

        assertThatThrownBy(() -> groupExpenseService.createExpense(groupId, request, leaderUserId))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_EXPENSE_BENEFICIARIES);
    }

    @Test
    void updateExpense_Success_ByLeader() {
        UUID expenseId = UUID.randomUUID();
        GroupExpense existing = new GroupExpense();
        existing.setGroupExpenseId(expenseId);
        existing.setGroupTrip(trip);
        existing.setPaidBy(regularMember);
        existing.setTitle("Tiền xăng cũ");
        existing.setAmount(new BigDecimal("200000.00"));
        existing.setBeneficiaryScope(BeneficiaryScope.ALL_MEMBERS);
        existing.setBeneficiaryCount(2);
        existing.setShares(new ArrayList<>());

        GroupExpenseUpdateRequest request = GroupExpenseUpdateRequest.builder()
                .title("Tiền xăng mới")
                .amount(new BigDecimal("220000.00"))
                .build();

        when(matchingGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(matchingMemberRepository.findByGroupIdAndUserId(groupId, leaderUserId))
                .thenReturn(Optional.of(leaderMember));
        when(groupExpenseRepository.findByGroupExpenseIdAndGroupTrip_MatchingGroup_MatchingGroupId(expenseId, groupId))
                .thenReturn(Optional.of(existing));
        when(matchingMemberRepository.findActiveMembers(groupId, JoinStatus.ACCEPTED))
                .thenReturn(List.of(leaderMember, regularMember));
        when(groupExpenseRepository.save(existing)).thenReturn(existing);

        GroupExpenseResponse response = groupExpenseService.updateExpense(groupId, expenseId, request, leaderUserId);

        assertThat(response).isNotNull();
        assertThat(existing.getTitle()).isEqualTo("Tiền xăng mới");
        assertThat(existing.getAmount()).isEqualByComparingTo("220000.00");
    }

    @Test
    void updateExpense_ThrowsWhenNotLeader() {
        UUID expenseId = UUID.randomUUID();

        GroupExpenseUpdateRequest request = GroupExpenseUpdateRequest.builder()
                .title("Sửa trộm")
                .build();

        when(matchingGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(matchingMemberRepository.findByGroupIdAndUserId(groupId, memberUserId))
                .thenReturn(Optional.of(regularMember)); // Regular member tries to edit expense

        assertThatThrownBy(() -> groupExpenseService.updateExpense(groupId, expenseId, request, memberUserId))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.UNAUTHORIZED_EXPENSE_ACTION);
    }

    @Test
    void voidExpense_Success_ByLeader() {
        UUID expenseId = UUID.randomUUID();
        GroupExpense existing = new GroupExpense();
        existing.setGroupExpenseId(expenseId);
        existing.setGroupTrip(trip);
        existing.setPaidBy(regularMember); // Paid by member, Leader voids it
        existing.setTitle("Khoản chi sai");
        existing.setShares(new ArrayList<>());

        when(matchingGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(matchingMemberRepository.findByGroupIdAndUserId(groupId, leaderUserId))
                .thenReturn(Optional.of(leaderMember));
        when(groupExpenseRepository.findByGroupExpenseIdAndGroupTrip_MatchingGroup_MatchingGroupId(expenseId, groupId))
                .thenReturn(Optional.of(existing));

        groupExpenseService.voidExpense(groupId, expenseId, leaderUserId);

        assertThat(existing.getIsDeleted()).isTrue();
        assertThat(existing.getDeletedAt()).isNotNull();
        verify(groupExpenseRepository).save(existing);
    }

    @Test
    void voidExpense_ThrowsWhenNotLeader() {
        UUID expenseId = UUID.randomUUID();

        when(matchingGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(matchingMemberRepository.findByGroupIdAndUserId(groupId, memberUserId))
                .thenReturn(Optional.of(regularMember));

        assertThatThrownBy(() -> groupExpenseService.voidExpense(groupId, expenseId, memberUserId))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.UNAUTHORIZED_EXPENSE_ACTION);
    }

    @Test
    void getExpenseSummary_Success() {
        when(matchingGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(matchingMemberRepository.findByGroupIdAndUserId(groupId, leaderUserId))
                .thenReturn(Optional.of(leaderMember));
        when(groupTripRepository.findByMatchingGroup(group)).thenReturn(Optional.of(trip));
        when(groupExpenseRepository.sumAmountByMatchingGroupId(groupId)).thenReturn(new BigDecimal("1500000.00"));
        when(groupExpenseRepository.countByMatchingGroupId(groupId)).thenReturn(3L);
        when(matchingMemberRepository.countActiveMembersByGroupIdAndStatus(groupId, JoinStatus.ACCEPTED)).thenReturn(3L);

        GroupExpenseSummaryResponse summary = groupExpenseService.getExpenseSummary(groupId, leaderUserId);

        assertThat(summary).isNotNull();
        assertThat(summary.getMatchingGroupId()).isEqualTo(groupId);
        assertThat(summary.getTotalExpenseAmount()).isEqualByComparingTo("1500000.00");
        assertThat(summary.getTotalExpensesCount()).isEqualTo(3);
        assertThat(summary.getActiveMemberCount()).isEqualTo(3);
        assertThat(summary.getAverageExpensePerMember()).isEqualByComparingTo("500000.00");
    }

    @Test
    void getGroupExpenses_Success() {
        GroupExpenseFilterRequest filter = new GroupExpenseFilterRequest();
        filter.setPage(0);
        filter.setSize(10);
        Pageable pageable = filter.getPageable();

        GroupExpense e1 = new GroupExpense();
        e1.setGroupExpenseId(UUID.randomUUID());
        e1.setGroupTrip(trip);
        e1.setPaidBy(leaderMember);
        e1.setTitle("Expense 1");
        e1.setAmount(new BigDecimal("100000.00"));

        Page<GroupExpense> page = new PageImpl<>(List.of(e1), pageable, 1);

        when(matchingGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(matchingMemberRepository.findByGroupIdAndUserId(groupId, leaderUserId))
                .thenReturn(Optional.of(leaderMember));
        when(groupExpenseRepository.findByGroupIdWithFilter(
                eq(groupId),
                isNull(),
                isNull(),
                isNull(),
                any(Pageable.class)
        )).thenReturn(page);

        PaginationResponse<GroupExpenseResponse> response = groupExpenseService.getGroupExpenses(groupId, filter, leaderUserId);

        assertThat(response).isNotNull();
        assertThat(response.getTotalElements()).isEqualTo(1);
        assertThat(response.getContent()).hasSize(1);
    }
}
