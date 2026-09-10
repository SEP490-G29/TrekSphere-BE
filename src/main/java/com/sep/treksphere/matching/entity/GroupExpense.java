package com.sep.treksphere.matching.entity;


import com.sep.treksphere.common.entity.BaseEntity;
import com.sep.treksphere.matching.enums.BeneficiaryScope;
import com.sep.treksphere.matching.enums.SplitMethod;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "group_expense")
@Getter
@Setter
@NoArgsConstructor
public class GroupExpense extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID groupExpenseId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_trip_id", nullable = false)
    private GroupTrip groupTrip;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paid_by", nullable = false)
    private MatchingMember paidBy;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BeneficiaryScope beneficiaryScope;

    @Column(nullable = false)
    private Integer beneficiaryCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SplitMethod splitMethod;

    @Column(nullable = false)
    private LocalDateTime spentAt;

    @Column(length = 500)
    private String receiptUrl;

    @Column(columnDefinition = "TEXT")
    private String note;
}
