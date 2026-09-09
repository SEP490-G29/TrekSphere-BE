package com.sep.treksphere.matching.entity;

import com.sep.treksphere.common.entity.BaseEntity;
import com.sep.treksphere.matching.enums.GroupContentStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
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

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "group_post_image", joinColumns = @JoinColumn(name = "group_post_id"))
    @Column(name = "image_url", length = 500)
    private List<String> imageUrls = new java.util.ArrayList<>();
}
