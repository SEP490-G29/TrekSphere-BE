package com.sep.treksphere.matching.entity;

import com.sep.treksphere.common.entity.BaseEntity;
import com.sep.treksphere.matching.enums.BeneficiaryScope;
import com.sep.treksphere.matching.enums.SplitMethod;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "group_expense")
@SQLRestriction("is_deleted = false")
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

    @OneToMany(mappedBy = "groupExpense", cascade = CascadeType.ALL)
    private List<GroupExpenseShare> shares = new ArrayList<>();
}
