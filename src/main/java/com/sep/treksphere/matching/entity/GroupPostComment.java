package com.sep.treksphere.matching.entity;

import com.sep.treksphere.common.entity.BaseEntity;
import com.sep.treksphere.matching.enums.GroupContentStatus;
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
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "group_post_comment")
@Getter
@Setter
@NoArgsConstructor
public class GroupPostComment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID groupPostCommentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_post_id", nullable = false)
    private GroupPost groupPost;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "answered_by", nullable = false)
    private MatchingMember answeredBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_comment_id")
    private GroupPostComment parentComment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reply_to_comment_id")
    private GroupPostComment replyToComment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reply_to_member_id")
    private MatchingMember replyToMember;

    @OneToMany(mappedBy = "parentComment", cascade = CascadeType.ALL)
    @OrderBy("createdAt ASC")
    private List<GroupPostComment> replies = new ArrayList<>();

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GroupContentStatus status = GroupContentStatus.SHOW;
}

