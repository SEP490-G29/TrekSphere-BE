package com.sep.treksphere.matching.grouptrip;

import com.sep.treksphere.common.entity.BaseEntity;
import com.sep.treksphere.matching.MatchingGroup;
import com.sep.treksphere.matching.member.MatchingMember;
import com.sep.treksphere.user.User;
import com.sep.treksphere.matching.grouptrip.MomentStatus;
import com.sep.treksphere.matching.grouptrip.MomentVisibility;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "group_moment")
@Getter
@Setter
@NoArgsConstructor
public class GroupMoment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID groupMomentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "matching_group_id", nullable = false)
    private MatchingGroup matchingGroup;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_matching_member_id", nullable = false)
    private MatchingMember authorMatchingMember;

    @Column(columnDefinition = "TEXT")
    private String caption;

    private LocalDateTime capturedAt;

    @Column(length = 200)
    private String placeName;

    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MomentVisibility visibility = MomentVisibility.GROUP_ONLY;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MomentStatus status = MomentStatus.VISIBLE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hidden_by_user_id")
    private User hiddenByUser;

    @Column(columnDefinition = "TEXT")
    private String hiddenReason;
}
