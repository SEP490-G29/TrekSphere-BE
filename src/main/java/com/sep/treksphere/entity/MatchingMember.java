package com.sep.treksphere.entity;

import com.sep.treksphere.enums.matching.JoinStatus;
import com.sep.treksphere.enums.matching.MatchingRole;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "matching_member", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"group_id", "user_id"})
})
@Getter
@Setter
@NoArgsConstructor


public class MatchingMember extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID matchingMemberId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private MatchingGroup matchingGroup;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private MatchingRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private JoinStatus status = JoinStatus.PENDING;
}
