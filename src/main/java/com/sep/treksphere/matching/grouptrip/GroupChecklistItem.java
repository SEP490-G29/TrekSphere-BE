package com.sep.treksphere.matching.grouptrip;

import com.sep.treksphere.common.entity.BaseEntity;
import com.sep.treksphere.matching.MatchingGroup;
import com.sep.treksphere.matching.member.MatchingMember;
import com.sep.treksphere.matching.grouptrip.ChecklistItemScope;
import com.sep.treksphere.matching.grouptrip.ChecklistItemStatus;
import com.sep.treksphere.matching.grouptrip.ChecklistItemType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "group_checklist_item")
@Getter
@Setter
@NoArgsConstructor
public class GroupChecklistItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID groupChecklistItemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "matching_group_id", nullable = false)
    private MatchingGroup matchingGroup;

    @Column(nullable = false, length = 200)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChecklistItemScope itemScope;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private ChecklistItemType itemTypeCode;

    @Column(nullable = false)
    private Boolean isRequired = false;

    @Column(columnDefinition = "TEXT")
    private String note;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_matching_member_id")
    private MatchingMember assigneeMatchingMember;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChecklistItemStatus status = ChecklistItemStatus.TODO;

    private LocalDateTime completedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "completed_by")
    private MatchingMember completedBy;
}
