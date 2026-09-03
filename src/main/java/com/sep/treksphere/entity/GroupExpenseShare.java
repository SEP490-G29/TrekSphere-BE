package com.sep.treksphere.entity;

import com.sep.treksphere.enums.group.ExpenseShareSettlementStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "group_expense_share", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"group_expense_id", "matching_member_id"})
})
@Getter
@Setter
@NoArgsConstructor
public class GroupExpenseShare extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID groupExpenseShareId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_expense_id", nullable = false)
    private GroupExpense groupExpense;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "matching_member_id", nullable = false)
    private MatchingMember matchingMember;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal shareAmount;

    @Column(length = 500)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ExpenseShareSettlementStatus settlementStatus = ExpenseShareSettlementStatus.UNSETTLED;

    private LocalDateTime settledAt;
}
