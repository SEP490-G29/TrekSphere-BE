package com.sep.treksphere.matching.service;

import com.sep.treksphere.common.exception.AppException;
import com.sep.treksphere.common.exception.ErrorCode;
import com.sep.treksphere.matching.dto.request.GroupSettlementProofRequest;
import com.sep.treksphere.matching.dto.request.GroupSettlementRejectRequest;
import com.sep.treksphere.matching.dto.response.GroupSettlementResponse;
import com.sep.treksphere.matching.dto.response.GroupSettlementSummaryResponse;
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
import com.sep.treksphere.matching.service.impl.GroupSettlementServiceImpl;
import com.sep.treksphere.notification.NotificationService;
import com.sep.treksphere.user.User;
import com.sep.treksphere.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GroupSettlementServiceTest {

    @Mock
    private MatchingGroupRepository matchingGroupRepository;

    @Mock
    private MatchingMemberRepository matchingMemberRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private GroupExpenseRepository groupExpenseRepository;

    @Mock
    private GroupExpenseShareRepository groupExpenseShareRepository;

    @Mock
    private GroupSettlementRepository groupSettlementRepository;

    @Mock
    private GroupTripRepository groupTripRepository;

    @Mock
    private NotificationService notificationService;

    @Spy
    private GroupSettlementMapper groupSettlementMapper = Mappers.getMapper(GroupSettlementMapper.class);

    @InjectMocks
    private GroupSettlementServiceImpl groupSettlementService;

    private UUID groupId;
    private MatchingGroup group;
    private GroupTrip groupTrip;
    private User leaderUser;
    private User memberUser1;
    private User memberUser2;
    private MatchingMember leaderMember;
    private MatchingMember member1;
    private MatchingMember member2;

    @BeforeEach
    void setUp() {
        groupId = UUID.randomUUID();
        group = new MatchingGroup();
        group.setMatchingGroupId(groupId);

        groupTrip = new GroupTrip();
        groupTrip.setGroupTripId(UUID.randomUUID());
        groupTrip.setMatchingGroup(group);
        groupTrip.setStatus(GroupTripStatus.PLANNED);

        leaderUser = new User();
        leaderUser.setUserId(UUID.randomUUID());
        leaderUser.setEmail("leader@test.com");
        leaderUser.setFullName("Leader User");

        memberUser1 = new User();
        memberUser1.setUserId(UUID.randomUUID());
        memberUser1.setEmail("member1@test.com");
        memberUser1.setFullName("Member One");

        memberUser2 = new User();
        memberUser2.setUserId(UUID.randomUUID());
        memberUser2.setEmail("member2@test.com");
        memberUser2.setFullName("Member Two");

        leaderMember = new MatchingMember();
        leaderMember.setMatchingMemberId(UUID.randomUUID());
        leaderMember.setMatchingGroup(group);
        leaderMember.setUser(leaderUser);
        leaderMember.setRole(MatchingRole.LEADER);
        leaderMember.setStatus(JoinStatus.ACCEPTED);

        member1 = new MatchingMember();
        member1.setMatchingMemberId(UUID.randomUUID());
        member1.setMatchingGroup(group);
        member1.setUser(memberUser1);
        member1.setRole(MatchingRole.MEMBER);
        member1.setStatus(JoinStatus.ACCEPTED);

        member2 = new MatchingMember();
        member2.setMatchingMemberId(UUID.randomUUID());
        member2.setMatchingGroup(group);
        member2.setUser(memberUser2);
        member2.setRole(MatchingRole.MEMBER);
        member2.setStatus(JoinStatus.ACCEPTED);
    }

    @Test
    @DisplayName("TC-SETTLE-01: Tính tổng kết số dư và gợi ý chuyển tiền tối giản (Greedy Netting)")
    void getSettlementSummary_Success_GreedyNetting() {
        // Given: Leader chi 600k chia đều cho 3 người (mỗi người 200k)
        // Leader: paid 600k, share 200k -> net +400k (Creditor)
        // Member1: paid 0, share 200k -> net -200k (Debtor)
        // Member2: paid 0, share 200k -> net -200k (Debtor)
        GroupExpense expense = new GroupExpense();
        expense.setGroupExpenseId(UUID.randomUUID());
        expense.setGroupTrip(groupTrip);
        expense.setAmount(new BigDecimal("600000"));
        expense.setPaidBy(leaderMember);

        GroupExpenseShare shareLeader = new GroupExpenseShare();
        shareLeader.setMatchingMember(leaderMember);
        shareLeader.setShareAmount(new BigDecimal("200000"));
        shareLeader.setIsDeleted(false);

        GroupExpenseShare share1 = new GroupExpenseShare();
        share1.setMatchingMember(member1);
        share1.setShareAmount(new BigDecimal("200000"));
        share1.setIsDeleted(false);

        GroupExpenseShare share2 = new GroupExpenseShare();
        share2.setMatchingMember(member2);
        share2.setShareAmount(new BigDecimal("200000"));
        share2.setIsDeleted(false);

        expense.setShares(List.of(shareLeader, share1, share2));

        when(matchingGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(userRepository.findByEmail(leaderUser.getEmail())).thenReturn(Optional.of(leaderUser));
        when(matchingMemberRepository.findByMatchingGroupAndUserAndIsDeletedFalse(group, leaderUser))
                .thenReturn(Optional.of(leaderMember));
        when(groupTripRepository.findByMatchingGroup(group)).thenReturn(Optional.of(groupTrip));
        when(matchingMemberRepository.findActiveMembers(groupId, JoinStatus.ACCEPTED))
                .thenReturn(List.of(leaderMember, member1, member2));
        when(groupExpenseRepository.findByGroupTrip_MatchingGroup_MatchingGroupId(groupId))
                .thenReturn(List.of(expense));
        when(groupSettlementRepository.findByGroupTrip_MatchingGroup_MatchingGroupIdAndIsDeletedFalseOrderByCreatedAtAsc(groupId))
                .thenReturn(Collections.emptyList());

        // When
        GroupSettlementSummaryResponse response = groupSettlementService.getSettlementSummary(groupId, leaderUser.getEmail());

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getTotalGroupExpense()).isEqualByComparingTo(new BigDecimal("600000"));
        assertThat(response.getMemberBalances()).hasSize(3);
        assertThat(response.getSuggestions()).hasSize(2);

        // Leader balance: +400k
        assertThat(response.getMemberBalances().stream()
                .filter(b -> b.getMember().getMatchingMemberId().equals(leaderMember.getMatchingMemberId()))
                .findFirst().get().getNetBalance()).isEqualByComparingTo(new BigDecimal("400000"));

        // Suggestions should be: Member1 -> Leader (200k) and Member2 -> Leader (200k)
        assertThat(response.getSuggestions())
                .allMatch(s -> s.getToMember().getMatchingMemberId().equals(leaderMember.getMatchingMemberId()));
    }

    @Test
    @DisplayName("TC-SETTLE-02: Không cho phép non-member xem tổng kết công nợ")
    void getSettlementSummary_Forbidden_WhenNotMember() {
        when(matchingGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(userRepository.findByEmail("outsider@test.com")).thenReturn(Optional.of(new User()));
        when(matchingMemberRepository.findByMatchingGroupAndUserAndIsDeletedFalse(any(), any()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> groupSettlementService.getSettlementSummary(groupId, "outsider@test.com"))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_ACCEPTED_MATCHING_MEMBER);
    }

    @Test
    @DisplayName("TC-SETTLE-03: Leader khởi tạo các lệnh quyết toán PENDING thành công")
    void generateSettlements_Success_LeaderOnly() {
        GroupExpense expense = new GroupExpense();
        expense.setAmount(new BigDecimal("300000"));
        expense.setPaidBy(leaderMember);

        GroupExpenseShare s1 = new GroupExpenseShare();
        s1.setMatchingMember(leaderMember);
        s1.setShareAmount(new BigDecimal("150000"));
        s1.setIsDeleted(false);

        GroupExpenseShare s2 = new GroupExpenseShare();
        s2.setMatchingMember(member1);
        s2.setShareAmount(new BigDecimal("150000"));
        s2.setIsDeleted(false);

        expense.setShares(List.of(s1, s2));

        when(matchingGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(userRepository.findByEmail(leaderUser.getEmail())).thenReturn(Optional.of(leaderUser));
        when(matchingMemberRepository.findByMatchingGroupAndUserAndIsDeletedFalse(group, leaderUser))
                .thenReturn(Optional.of(leaderMember));
        when(groupTripRepository.findByMatchingGroup(group)).thenReturn(Optional.of(groupTrip));
        when(matchingMemberRepository.findActiveMembers(groupId, JoinStatus.ACCEPTED))
                .thenReturn(List.of(leaderMember, member1));

        when(groupExpenseRepository.findByGroupTrip_MatchingGroup_MatchingGroupId(groupId))
                .thenReturn(List.of(expense));
        when(groupSettlementRepository.findByGroupTrip_MatchingGroup_MatchingGroupIdAndIsDeletedFalseOrderByCreatedAtAsc(groupId))
                .thenReturn(Collections.emptyList());
        when(groupSettlementRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        List<GroupSettlementResponse> result = groupSettlementService.generateSettlements(groupId, leaderUser.getEmail());

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getFromMember().getMatchingMemberId()).isEqualTo(member1.getMatchingMemberId());
        assertThat(result.get(0).getToMember().getMatchingMemberId()).isEqualTo(leaderMember.getMatchingMemberId());
        assertThat(result.get(0).getAmount()).isEqualByComparingTo(new BigDecimal("150000"));
        assertThat(result.get(0).getStatus()).isEqualTo(SettlementStatus.PENDING);
    }

    @Test
    @DisplayName("TC-SETTLE-04: Thành viên thường không được khởi tạo danh sách quyết toán")
    void generateSettlements_Forbidden_WhenNotLeader() {
        when(matchingGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(userRepository.findByEmail(memberUser1.getEmail())).thenReturn(Optional.of(memberUser1));
        when(matchingMemberRepository.findByMatchingGroupAndUserAndIsDeletedFalse(group, memberUser1))
                .thenReturn(Optional.of(member1));

        assertThatThrownBy(() -> groupSettlementService.generateSettlements(groupId, memberUser1.getEmail()))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.UNAUTHORIZED_EXPENSE_ACTION);
    }

    @Test
    @DisplayName("TC-SETTLE-05: Người nợ nộp chứng từ chuyển tiền thành công -> PROOF_SUBMITTED")
    void submitProof_Success_ByDebtor() {
        UUID settlementId = UUID.randomUUID();
        GroupSettlement settlement = new GroupSettlement();
        settlement.setGroupSettlementId(settlementId);
        settlement.setGroupTrip(groupTrip);
        settlement.setFromMatchingMember(member1); // Debtor
        settlement.setToMatchingMember(leaderMember); // Payee
        settlement.setAmount(new BigDecimal("150000"));
        settlement.setStatus(SettlementStatus.PENDING);

        when(matchingGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(userRepository.findByEmail(memberUser1.getEmail())).thenReturn(Optional.of(memberUser1));
        when(matchingMemberRepository.findByMatchingGroupAndUserAndIsDeletedFalse(group, memberUser1))
                .thenReturn(Optional.of(member1));
        when(groupSettlementRepository.findByGroupSettlementIdAndGroupTrip_MatchingGroup_MatchingGroupIdAndIsDeletedFalse(settlementId, groupId))
                .thenReturn(Optional.of(settlement));
        when(groupSettlementRepository.save(any(GroupSettlement.class))).thenAnswer(i -> i.getArgument(0));

        GroupSettlementProofRequest req = GroupSettlementProofRequest.builder()
                .proofUrl("https://img.treksphere.com/receipts/bill123.jpg")
                .build();

        // When
        GroupSettlementResponse response = groupSettlementService.submitProof(groupId, settlementId, req, memberUser1.getEmail());

        // Then
        assertThat(response.getStatus()).isEqualTo(SettlementStatus.PROOF_SUBMITTED);
        assertThat(response.getProofUrl()).isEqualTo("https://img.treksphere.com/receipts/bill123.jpg");
        assertThat(response.getSubmittedAt()).isNotNull();
        verify(notificationService).notify(
                org.mockito.ArgumentMatchers.eq(leaderMember.getUser().getUserId()),
                org.mockito.ArgumentMatchers.eq(com.sep.treksphere.notification.NotificationEventType.GROUP_SETTLEMENT_PROOF_SUBMITTED),
                org.mockito.ArgumentMatchers.eq(com.sep.treksphere.notification.ReferenceType.GROUP_EXPENSE),
                org.mockito.ArgumentMatchers.eq(settlementId),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("TC-SETTLE-06: Người khác không được nộp chứng từ thay người nợ")
    void submitProof_Forbidden_WhenNotDebtor() {
        UUID settlementId = UUID.randomUUID();
        GroupSettlement settlement = new GroupSettlement();
        settlement.setGroupSettlementId(settlementId);
        settlement.setGroupTrip(groupTrip);
        settlement.setFromMatchingMember(member1); // Debtor là Member1
        settlement.setToMatchingMember(leaderMember);

        when(matchingGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(userRepository.findByEmail(memberUser2.getEmail())).thenReturn(Optional.of(memberUser2));
        when(matchingMemberRepository.findByMatchingGroupAndUserAndIsDeletedFalse(group, memberUser2))
                .thenReturn(Optional.of(member2)); // Member2 nộp thay
        when(groupSettlementRepository.findByGroupSettlementIdAndGroupTrip_MatchingGroup_MatchingGroupIdAndIsDeletedFalse(settlementId, groupId))
                .thenReturn(Optional.of(settlement));

        GroupSettlementProofRequest req = GroupSettlementProofRequest.builder()
                .proofUrl("https://img.treksphere.com/receipts/bill123.jpg")
                .build();

        assertThatThrownBy(() -> groupSettlementService.submitProof(groupId, settlementId, req, memberUser2.getEmail()))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.UNAUTHORIZED_SETTLEMENT_SUBMIT);
    }

    @Test
    @DisplayName("TC-SETTLE-07: Người nhận tiền (Payee) xác nhận quyết toán thành công -> CONFIRMED")
    void confirmSettlement_Success_ByPayee() {
        UUID settlementId = UUID.randomUUID();
        GroupSettlement settlement = new GroupSettlement();
        settlement.setGroupSettlementId(settlementId);
        settlement.setGroupTrip(groupTrip);
        settlement.setFromMatchingMember(member1);
        settlement.setToMatchingMember(leaderMember); // Payee là Leader
        settlement.setAmount(new BigDecimal("150000"));
        settlement.setStatus(SettlementStatus.PROOF_SUBMITTED);

        when(matchingGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(userRepository.findByEmail(leaderUser.getEmail())).thenReturn(Optional.of(leaderUser));
        when(matchingMemberRepository.findByMatchingGroupAndUserAndIsDeletedFalse(group, leaderUser))
                .thenReturn(Optional.of(leaderMember));
        when(groupSettlementRepository.findByGroupSettlementIdAndGroupTrip_MatchingGroup_MatchingGroupIdAndIsDeletedFalse(settlementId, groupId))
                .thenReturn(Optional.of(settlement));
        when(groupSettlementRepository.save(any(GroupSettlement.class))).thenAnswer(i -> i.getArgument(0));
        when(groupSettlementRepository.findByGroupTrip_MatchingGroup_MatchingGroupIdAndIsDeletedFalseOrderByCreatedAtAsc(groupId))
                .thenReturn(List.of(settlement));

        // When
        GroupSettlementResponse response = groupSettlementService.confirmSettlement(groupId, settlementId, leaderUser.getEmail());

        // Then
        assertThat(response.getStatus()).isEqualTo(SettlementStatus.CONFIRMED);
        assertThat(response.getConfirmedAt()).isNotNull();
        assertThat(response.getConfirmedBy().getMatchingMemberId()).isEqualTo(leaderMember.getMatchingMemberId());
        verify(notificationService).notify(
                org.mockito.ArgumentMatchers.eq(member1.getUser().getUserId()),
                org.mockito.ArgumentMatchers.eq(com.sep.treksphere.notification.NotificationEventType.GROUP_SETTLEMENT_CONFIRMED),
                org.mockito.ArgumentMatchers.eq(com.sep.treksphere.notification.ReferenceType.GROUP_EXPENSE),
                org.mockito.ArgumentMatchers.eq(settlementId),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("TC-SETTLE-08: Người nợ không được tự ý confirm lệnh quyết toán của mình")
    void confirmSettlement_Forbidden_WhenNotPayee() {
        UUID settlementId = UUID.randomUUID();
        GroupSettlement settlement = new GroupSettlement();
        settlement.setGroupSettlementId(settlementId);
        settlement.setGroupTrip(groupTrip);
        settlement.setFromMatchingMember(member1); // Debtor
        settlement.setToMatchingMember(leaderMember); // Payee

        when(matchingGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(userRepository.findByEmail(memberUser1.getEmail())).thenReturn(Optional.of(memberUser1));
        when(matchingMemberRepository.findByMatchingGroupAndUserAndIsDeletedFalse(group, memberUser1))
                .thenReturn(Optional.of(member1)); // Member1 tự confirm
        when(groupSettlementRepository.findByGroupSettlementIdAndGroupTrip_MatchingGroup_MatchingGroupIdAndIsDeletedFalse(settlementId, groupId))
                .thenReturn(Optional.of(settlement));

        assertThatThrownBy(() -> groupSettlementService.confirmSettlement(groupId, settlementId, memberUser1.getEmail()))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.UNAUTHORIZED_SETTLEMENT_CONFIRM);
    }

    @Test
    @DisplayName("TC-SETTLE-09: Người nhận tiền (Payee) từ chối quyết toán -> REJECTED kèm lý do")
    void rejectSettlement_Success_ByPayee() {
        UUID settlementId = UUID.randomUUID();
        GroupSettlement settlement = new GroupSettlement();
        settlement.setGroupSettlementId(settlementId);
        settlement.setGroupTrip(groupTrip);
        settlement.setFromMatchingMember(member1);
        settlement.setToMatchingMember(leaderMember);
        settlement.setAmount(new BigDecimal("150000"));
        settlement.setStatus(SettlementStatus.PROOF_SUBMITTED);

        when(matchingGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(userRepository.findByEmail(leaderUser.getEmail())).thenReturn(Optional.of(leaderUser));
        when(matchingMemberRepository.findByMatchingGroupAndUserAndIsDeletedFalse(group, leaderUser))
                .thenReturn(Optional.of(leaderMember));
        when(groupSettlementRepository.findByGroupSettlementIdAndGroupTrip_MatchingGroup_MatchingGroupIdAndIsDeletedFalse(settlementId, groupId))
                .thenReturn(Optional.of(settlement));
        when(groupSettlementRepository.save(any(GroupSettlement.class))).thenAnswer(i -> i.getArgument(0));

        GroupSettlementRejectRequest req = GroupSettlementRejectRequest.builder()
                .reason("Chưa nhận được tiền vào tài khoản ngân hàng")
                .build();

        // When
        GroupSettlementResponse response = groupSettlementService.rejectSettlement(groupId, settlementId, req, leaderUser.getEmail());

        // Then
        assertThat(response.getStatus()).isEqualTo(SettlementStatus.REJECTED);
        assertThat(response.getRejectReason()).isEqualTo("Chưa nhận được tiền vào tài khoản ngân hàng");
    }

}
