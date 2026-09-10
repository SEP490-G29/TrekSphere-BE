package com.sep.treksphere.matching.entity;


import com.sep.treksphere.common.entity.BaseEntity;
import com.sep.treksphere.matching.enums.SettlementStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "group_settlement")
@Getter
@Setter
@NoArgsConstructor
public class GroupSettlement extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID groupSettlementId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_trip_id", nullable = false)
    private GroupTrip groupTrip;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_matching_member_id", nullable = false)
    private MatchingMember fromMatchingMember;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_matching_member_id", nullable = false)
    private MatchingMember toMatchingMember;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SettlementStatus status = SettlementStatus.PENDING;

    @Column(length = 500)
    private String proofUrl;

    private LocalDateTime submittedAt;

    private LocalDateTime confirmedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "confirmed_by")
    private MatchingMember confirmedBy;

    @Column(columnDefinition = "TEXT")
    private String rejectReason;
}
