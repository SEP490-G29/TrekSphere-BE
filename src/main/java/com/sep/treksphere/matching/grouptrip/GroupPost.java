package com.sep.treksphere.matching.grouptrip;

import com.sep.treksphere.common.entity.BaseEntity;
import com.sep.treksphere.matching.MatchingGroup;
import com.sep.treksphere.matching.member.MatchingMember;
import com.sep.treksphere.matching.grouptrip.GroupContentStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "group_post")
@Getter
@Setter
@NoArgsConstructor
public class GroupPost extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID groupPostId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "matching_group_id", nullable = false)
    private MatchingGroup matchingGroup;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "posted_by", nullable = false)
    private MatchingMember postedBy;

    @Column(length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GroupContentStatus status = GroupContentStatus.SHOW;
}
